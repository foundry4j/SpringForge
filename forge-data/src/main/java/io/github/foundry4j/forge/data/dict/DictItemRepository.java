package io.github.foundry4j.forge.data.dict;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/// Repository of [DictItem].
public interface DictItemRepository extends JpaRepository<DictItem, Long> {

    /// Returns the enabled items of a dictionary ordered by their sort number.
    List<DictItem> findByDictTypeCodeAndEnabledTrueOrderBySortNoAsc(String dictTypeCode);
}
