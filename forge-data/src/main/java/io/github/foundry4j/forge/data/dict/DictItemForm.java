package io.github.foundry4j.forge.data.dict;

/// Mutable input of a dictionary item.
///
/// @param label       label shown to users, mandatory
/// @param value       stored value, mandatory and unique inside the dictionary
/// @param sortNo      sort order, defaults to 0
/// @param enabled     whether the item is enabled, defaults to `true`
/// @param extra       optional JSON payload
/// @param parentCode  value of the parent item, `null` for roots
/// @param level       depth inside the tree, defaults to 1
/// @param description optional description
public record DictItemForm(String label, String value, Integer sortNo, Boolean enabled,
                           String extra, String parentCode, Integer level, String description) {

    /// Applies the defaults.
    public DictItemForm {
        sortNo = sortNo == null ? 0 : sortNo;
        enabled = enabled == null || enabled;
        level = level == null || level < 1 ? 1 : level;
    }
}
