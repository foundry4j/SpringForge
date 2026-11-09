package io.github.foundry4j.forge.data.dict;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Reads, caches and maintains data dictionary entries.
///
/// Lookups are cached with Spring Cache. With the tiered cache of `forge-cache`
/// enabled, repeated lookups never touch the database; every mutation evicts the
/// affected dictionary so the next read observes the change.
public class DictService {

    /// Name of the cache holding dictionary items.
    public static final String CACHE_ITEMS = "forge:dict:items";

    /// Name of the cache holding label lookups.
    public static final String CACHE_LABELS = "forge:dict:labels";

    private final DictTypeRepository typeRepository;
    private final DictItemRepository itemRepository;

    /// Creates the service.
    public DictService(DictTypeRepository typeRepository, DictItemRepository itemRepository) {
        this.typeRepository = typeRepository;
        this.itemRepository = itemRepository;
    }

    /// Returns the enabled items of a dictionary ordered by `sortNo`.
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_ITEMS, key = "#typeCode")
    public List<DictItemView> items(String typeCode) {
        return itemRepository.findByDictTypeCodeAndEnabledTrueOrderBySortNoAsc(typeCode)
                .stream()
                .map(DictItemView::from)
                .toList();
    }

    /// Returns every item of a dictionary, disabled ones included, for admin screens.
    @Transactional(readOnly = true)
    public List<DictItemView> allItems(String typeCode) {
        return itemRepository.findByDictTypeCodeOrderBySortNoAsc(typeCode)
                .stream()
                .map(DictItemView::from)
                .toList();
    }

    /// Returns the items of a dictionary indexed by their value.
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CACHE_LABELS, key = "#typeCode")
    public Map<String, DictItemView> values(String typeCode) {
        return items(typeCode).stream()
                .collect(Collectors.toUnmodifiableMap(DictItemView::value, Function.identity(), (first, _) -> first));
    }

    /// Returns the label of a value, or `fallback` when the value is unknown.
    public String label(String typeCode, String value, String fallback) {
        DictItemView item = values(typeCode).get(value);
        return item == null ? fallback : item.label();
    }

    /// Returns the label of a value, or the value itself when unknown.
    public String label(String typeCode, String value) {
        return label(typeCode, value, value);
    }

    /// Translates a stored value into its label, returning `null` when unknown.
    public String translate(String typeCode, String value) {
        DictItemView item = values(typeCode).get(value);
        return item == null ? null : item.label();
    }

    /// Evicts every cached entry of a dictionary after it changed.
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#typeCode")
    public void evict(String typeCode) {
        // annotation driven
    }

    /// Returns whether a dictionary type exists and is enabled.
    @Transactional(readOnly = true)
    public boolean exists(String typeCode) {
        return typeRepository.findByCode(typeCode)
                .filter(DictType::isEnabled)
                .isPresent();
    }

    /// Returns every dictionary type ordered by code.
    @Transactional(readOnly = true)
    public List<DictType> types() {
        return typeRepository.findAllByOrderByCodeAsc();
    }

    /// Returns one dictionary type.
    ///
    /// @throws BusinessException when the type does not exist
    @Transactional(readOnly = true)
    public DictType type(String code) {
        return requireType(code);
    }

    /// Creates a dictionary type.
    ///
    /// @throws BusinessException when `code` or `name` is blank, or the code already exists
    @Transactional
    public DictType createType(DictTypeForm form) {
        requireForm(form);
        String code = requireText(form.code(), "Dictionary code");
        String name = requireText(form.name(), "Dictionary name");
        typeRepository.findByCode(code).ifPresent(_ -> {
            throw new BusinessException(CommonErrorCode.CONFLICT, "Dictionary already exists: " + code);
        });

        DictType type = new DictType();
        type.setCode(code);
        type.setName(name);
        type.setRemark(form.remark());
        type.setEnabled(form.enabled() == null || form.enabled());
        return typeRepository.save(type);
    }

    /// Updates a dictionary type; a `null` code keeps the current one.
    ///
    /// @throws BusinessException when the type does not exist
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#code")
    public DictType updateType(String code, DictTypeForm form) {
        requireForm(form);
        DictType type = requireType(code);
        if (form.code() != null && !form.code().isBlank() && !form.code().equals(code)) {
            typeRepository.findByCode(form.code()).ifPresent(_ -> {
                throw new BusinessException(CommonErrorCode.CONFLICT, "Dictionary already exists: " + form.code());
            });
            type.setCode(form.code());
        }
        type.setName(requireText(form.name(), "Dictionary name"));
        type.setRemark(form.remark());
        type.setEnabled(form.enabled() == null || form.enabled());
        return typeRepository.save(type);
    }

    /// Deletes a dictionary type and every item it owns.
    ///
    /// @throws BusinessException when the type does not exist
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#code")
    public void deleteType(String code) {
        DictType type = requireType(code);
        itemRepository.deleteAll(itemRepository.findByDictTypeCodeOrderBySortNoAsc(code));
        typeRepository.delete(type);
    }

    /// Creates an item inside an existing dictionary.
    ///
    /// @throws BusinessException when the type does not exist, the value already exists
    ///                           or the parent item is unknown
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#typeCode")
    public DictItemView createItem(String typeCode, DictItemForm form) {
        requireForm(form);
        requireType(typeCode);
        String value = requireText(form.value(), "Item value");
        itemRepository.findByDictTypeCodeAndValue(typeCode, value).ifPresent(_ -> {
            throw new BusinessException(CommonErrorCode.CONFLICT,
                    "Dictionary item already exists: " + typeCode + "/" + value);
        });
        requireParent(typeCode, form.parentCode());

        DictItem item = new DictItem();
        item.setDictTypeCode(typeCode);
        apply(item, form);
        return DictItemView.from(itemRepository.save(item));
    }

    /// Updates an item selected by its value; the value itself is immutable.
    ///
    /// @throws BusinessException when the item or its parent does not exist
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#typeCode")
    public DictItemView updateItem(String typeCode, String value, DictItemForm form) {
        requireForm(form);
        DictItem item = requireItem(typeCode, value);
        requireParent(typeCode, form.parentCode());
        apply(item, form);
        return DictItemView.from(itemRepository.save(item));
    }

    /// Deletes an item selected by its value.
    ///
    /// @throws BusinessException when the item does not exist
    @Transactional
    @CacheEvict(cacheNames = {CACHE_ITEMS, CACHE_LABELS}, key = "#typeCode")
    public void deleteItem(String typeCode, String value) {
        itemRepository.delete(requireItem(typeCode, value));
    }

    private DictItem requireItem(String typeCode, String value) {
        return itemRepository.findByDictTypeCodeAndValue(typeCode, value)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND,
                        "Dictionary item not found: " + typeCode + "/" + value));
    }

    private DictType requireType(String code) {
        return typeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND,
                        "Dictionary not found: " + code));
    }

    private void requireParent(String typeCode, String parentCode) {
        if (parentCode != null && !parentCode.isBlank() && itemRepository
                .findByDictTypeCodeAndValue(typeCode, parentCode).isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                    "Parent dictionary item not found: " + typeCode + "/" + parentCode);
        }
    }

    private static void apply(DictItem item, DictItemForm form) {
        item.setLabel(requireText(form.label(), "Item label"));
        item.setValue(requireText(form.value(), "Item value"));
        item.setSortNo(form.sortNo());
        item.setEnabled(form.enabled() == null || form.enabled());
        item.setExtra(form.extra());
        item.setParentCode(form.parentCode() == null || form.parentCode().isBlank() ? null : form.parentCode());
        item.setLevel(form.level());
        item.setDescription(form.description());
    }

    private static void requireForm(Object form) {
        if (form == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing request body");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, field + " must not be blank");
        }
        return value.trim();
    }
}
