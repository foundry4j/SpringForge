package io.github.foundry4j.forge.data.dict;

/// Mutable input of a dictionary type.
///
/// @param code    unique code, mandatory on create
/// @param name    display name, mandatory
/// @param remark  optional remark
/// @param enabled whether the dictionary is enabled, defaults to `true`
public record DictTypeForm(String code, String name, String remark, Boolean enabled) {
}
