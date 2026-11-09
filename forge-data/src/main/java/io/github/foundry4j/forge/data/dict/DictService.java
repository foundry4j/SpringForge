package io.github.foundry4j.forge.data.dict;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/// Reads and caches data dictionary entries.
///
/// Results are cached with Spring Cache. With the tiered cache of `forge-cache`
/// enabled, repeated lookups never touch the database; evicting a type is enough to
/// refresh every item of that dictionary.
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
}
