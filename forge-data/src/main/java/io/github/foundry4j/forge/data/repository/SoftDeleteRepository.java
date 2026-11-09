package io.github.foundry4j.forge.data.repository;

import io.github.foundry4j.forge.core.result.PageResult;
import io.github.foundry4j.forge.data.entity.BaseEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/// Repository base class with a recycle bin for soft deleted rows.
///
/// [io.github.foundry4j.forge.data.entity.BaseEntity] is annotated with Hibernate's
/// `@SoftDelete`, so `delete` only flips the `deleted` flag and every derived query
/// filters deleted rows out automatically. This class adds the missing half of the
/// feature: looking at and restoring deleted rows.
///
/// ```java
/// public interface OrderRepository extends BaseRepository<Order> { }
/// ```
///
/// Because Hibernate has no way to disable the soft delete filter, the recycle bin
/// methods use native SQL. They therefore bypass tenant filtering as well; call them
/// from an administrative context only.
///
/// @param <T> soft delete enabled entity type
@NoRepositoryBean
public abstract class SoftDeleteRepository<T extends BaseEntity>
        extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    private final EntityManager entityManager;
    private final Class<T> domainClass;
    private final String tableName;

    /// Creates the repository; Spring Data injects the domain class and entity manager.
    protected SoftDeleteRepository(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.domainClass = domainClass;
        this.entityManager = entityManager;
        Table table = domainClass.getAnnotation(Table.class);
        this.tableName = table != null && !table.name().isBlank()
                ? table.name()
                : domainClass.getSimpleName();
    }

    /// Creates the repository from entity information.
    protected SoftDeleteRepository(JpaEntityInformation<T, ?> information, EntityManager entityManager) {
        this(information.getJavaType(), entityManager);
    }

    /// Returns every row, including the soft deleted ones.
    @Transactional(readOnly = true)
    public List<T> findAllIncludingDeleted() {
        return query("select * from " + tableName).getResultList();
    }

    /// Finds a row by identifier, including soft deleted rows.
    @Transactional(readOnly = true)
    public Optional<T> findByIdIncludingDeleted(Long id) {
        return query("select * from " + tableName + " where id = ?1")
                .setParameter(1, id)
                .getResultList().stream().findFirst();
    }

    /// Pages over every row, including the soft deleted ones.
    @Transactional(readOnly = true)
    public PageResult<T> pageAllIncludingDeleted(int page, int size) {
        int pageIndex = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        Number total = (Number) entityManager
                .createNativeQuery("select count(*) from " + tableName)
                .getSingleResult();
        @SuppressWarnings("unchecked")
        List<T> content = query("select * from " + tableName)
                .setFirstResult((pageIndex - 1) * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        return PageResult.of(content, total.longValue(), pageIndex, pageSize);
    }

    /// Restores a soft deleted row.
    ///
    /// @return `true` when the row existed and was restored
    @Transactional
    public boolean restoreById(Long id) {
        int updated = entityManager
                .createNativeQuery("update " + tableName + " set deleted = false where id = ?1 and deleted = true")
                .setParameter(1, id)
                .executeUpdate();
        entityManager.clear();
        return updated > 0;
    }

    /// Physically deletes a row, including its soft deleted state.
    ///
    /// @return `true` when a row was removed
    @Transactional
    public boolean purgeById(Long id) {
        int removed = entityManager
                .createNativeQuery("delete from " + tableName + " where id = ?1")
                .setParameter(1, id)
                .executeUpdate();
        entityManager.clear();
        return removed > 0;
    }

    /// Physically deletes every row of the table.
    ///
    /// @return number of removed rows
    @Transactional
    public long purgeAll() {
        long removed = entityManager
                .createNativeQuery("delete from " + tableName)
                .executeUpdate();
        entityManager.clear();
        return removed;
    }

    @SuppressWarnings("unchecked")
    private Query query(String sql) {
        return entityManager.createNativeQuery(sql, domainClass);
    }
}
