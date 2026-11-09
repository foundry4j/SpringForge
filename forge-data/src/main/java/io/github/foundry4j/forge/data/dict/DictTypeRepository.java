package io.github.foundry4j.forge.data.dict;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/// Repository of [DictType].
public interface DictTypeRepository extends JpaRepository<DictType, Long> {

    /// Finds a dictionary type by its unique code.
    Optional<DictType> findByCode(String code);

    /// Returns every dictionary type ordered by code.
    List<DictType> findAllByOrderByCodeAsc();
}
