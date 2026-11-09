package io.github.foundry4j.forge.data.dict;

/// Read only projection of a dictionary item.
///
/// @param label       label shown to users
/// @param value       stored value
/// @param sortNo      sort order
/// @param extra       optional JSON payload
/// @param parentCode  value of the parent item, `null` for roots
/// @param level       depth inside the tree, roots are level 1
/// @param description optional description
public record DictItemView(String label, String value, int sortNo, String extra,
                           String parentCode, int level, String description) {

    /// Maps an entity to its view representation.
    public static DictItemView from(DictItem item) {
        return new DictItemView(item.getLabel(), item.getValue(), item.getSortNo(), item.getExtra(),
                item.getParentCode(), item.getLevel(), item.getDescription());
    }
}
