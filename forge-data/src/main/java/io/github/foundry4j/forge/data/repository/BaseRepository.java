package io.github.foundry4j.forge.data.repository;

import io.github.foundry4j.forge.core.exception.Errors;
import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.data.entity.BaseEntity;
import io.github.foundry4j.forge.data.support.PageResults;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/// Repository base interface for framework entities.
///
/// It adds two conveniences on top of [JpaRepository]: paging that directly
/// returns the framework neutral [PageResult] and `requireById`, which converts an
/// empty optional into a `404` business exception.
///
/// ```java
/// public interface OrderRepository extends BaseRepository<Order> {
///     PageResult<Order> findByStatus(String status, Pageable pageable);
/// }
/// ```
///
/// @param <T> entity type
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long> {

    /// Returns the entity or throws a `NOT_FOUND` business exception.
    default T requireById(Long id) {
        return findById(id).orElseThrow(() -> Errors.notFound("Resource", id));
    }

    /// Finds one page of entities, the page index starts at one.
    default PageResult<T> pageAll(int page, int size) {
        return pageAll(page, size, Sort.unsorted());
    }

    /// Finds one page of entities with an explicit sort, the page index starts at one.
    default PageResult<T> pageAll(int page, int size, Sort sort) {
        int pageIndex = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(pageIndex - 1, pageSize, sort);
        return PageResults.of(findAll(pageable));
    }
}
