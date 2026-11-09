package io.github.foundry4j.forge.data.dict;

import io.github.foundry4j.forge.data.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/// Single item of a dictionary.
@Entity
@Table(name = "forge_dict_item",
        indexes = @Index(name = "idx_forge_dict_item_type", columnList = "dict_type_code"))
public class DictItem extends BaseEntity {

    @Column(name = "dict_type_code", nullable = false, length = 64)
    private String dictTypeCode;

    @Column(name = "label", nullable = false, length = 128)
    private String label;

    @Column(name = "item_value", nullable = false, length = 128)
    private String value;

    @Column(name = "sort_no", nullable = false)
    private int sortNo;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "extra", length = 1024)
    private String extra;

    /// Code of the owning dictionary.
    public String getDictTypeCode() {
        return dictTypeCode;
    }

    /// Sets the owning dictionary code.
    public void setDictTypeCode(String dictTypeCode) {
        this.dictTypeCode = dictTypeCode;
    }

    /// Label shown to users.
    public String getLabel() {
        return label;
    }

    /// Sets the label.
    public void setLabel(String label) {
        this.label = label;
    }

    /// Stored value.
    public String getValue() {
        return value;
    }

    /// Sets the value.
    public void setValue(String value) {
        this.value = value;
    }

    /// Sort order inside the dictionary.
    public int getSortNo() {
        return sortNo;
    }

    /// Sets the sort order.
    public void setSortNo(int sortNo) {
        this.sortNo = sortNo;
    }

    /// Whether the item is enabled.
    public boolean isEnabled() {
        return enabled;
    }

    /// Enables or disables the item.
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /// Optional JSON payload with extra attributes.
    public String getExtra() {
        return extra;
    }

    /// Sets the extra payload.
    public void setExtra(String extra) {
        this.extra = extra;
    }
}
