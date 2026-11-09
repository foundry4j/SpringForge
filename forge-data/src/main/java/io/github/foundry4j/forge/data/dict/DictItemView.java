package io.github.foundry4j.forge.data.dict;

/// Read only projection of a dictionary item.
///
/// @param label  label shown to users
/// @param value  stored value
/// @param sortNo sort order
/// @param extra  optional JSON payload
public record DictItemView(String label, String value, int sortNo, String extra) {

    /// Maps an entity to its view representation.
    public static DictItemView from(DictItem item) {
        return new DictItemView(item.getLabel(), item.getValue(), item.getSortNo(), item.getExtra());
    }
}
