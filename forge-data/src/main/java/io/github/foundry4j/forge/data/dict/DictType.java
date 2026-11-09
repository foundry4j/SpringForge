package io.github.foundry4j.forge.data.dict;

import io.github.foundry4j.forge.data.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/// Dictionary type, for example `order_status`.
@Entity
@Table(name = "forge_dict_type",
        uniqueConstraints = @UniqueConstraint(name = "uk_forge_dict_type_code", columnNames = "code"))
public class DictType extends BaseEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /// Unique code, for example `order_status`.
    public String getCode() {
        return code;
    }

    /// Sets the unique code.
    public void setCode(String code) {
        this.code = code;
    }

    /// Display name of the dictionary.
    public String getName() {
        return name;
    }

    /// Sets the display name.
    public void setName(String name) {
        this.name = name;
    }

    /// Free form remark.
    public String getRemark() {
        return remark;
    }

    /// Sets the remark.
    public void setRemark(String remark) {
        this.remark = remark;
    }

    /// Whether the dictionary is enabled.
    public boolean isEnabled() {
        return enabled;
    }

    /// Enables or disables the dictionary.
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
