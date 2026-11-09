package io.github.foundry4j.forge.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import org.hibernate.annotations.SoftDelete;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/// Base class for every persistent entity of the framework.
///
/// It provides a surrogate primary key, audit columns, optimistic locking and soft
/// deletion. `@SoftDelete` is inherited by all subclasses, so `delete()` issues an
/// `UPDATE ... SET deleted = true` and every criteria query automatically filters
/// deleted rows out.
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SoftDelete
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /// Surrogate primary key.
    public Long getId() {
        return id;
    }

    /// Sets the surrogate primary key.
    public void setId(Long id) {
        this.id = id;
    }

    /// Creation timestamp, filled by Spring Data auditing.
    public Instant getCreatedAt() {
        return createdAt;
    }

    /// Last modification timestamp, filled by Spring Data auditing.
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /// Identifier of the creator, filled by Spring Data auditing.
    public String getCreatedBy() {
        return createdBy;
    }

    /// Identifier of the last modifier, filled by Spring Data auditing.
    public String getUpdatedBy() {
        return updatedBy;
    }

    /// Optimistic locking version.
    public long getVersion() {
        return version;
    }

    /// Sets the optimistic locking version.
    public void setVersion(long version) {
        this.version = version;
    }

    /// Whether the row is new.
    public boolean isNew() {
        return id == null;
    }
}
