package io.github.foundry4j.forge.data.dict;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/// Repository of [DictItem].
public interface DictItemRepository extends JpaRepository<DictItem, Long> {

    /// Returns the enabled items of a dictionary ordered by their sort number.
    List<DictItem> findByDictTypeCodeAndEnabledTrueOrderBySortNoAsc(String dictTypeCode);

    /// Returns every item of a dictionary ordered by sort number, disabled ones included.
    List<DictItem> findByDictTypeCodeOrderBySortNoAsc(String dictTypeCode);

    /// Finds one item by dictionary code and stored value.
    Optional<DictItem> findByDictTypeCodeAndValue(String dictTypeCode, String value);
}
