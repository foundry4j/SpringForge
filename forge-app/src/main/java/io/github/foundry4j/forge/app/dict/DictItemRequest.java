package io.github.foundry4j.forge.app.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/// Request body of dictionary item create and update calls.
///
/// @param label       label shown to users
/// @param value       stored value, unique inside the dictionary
/// @param sortNo      sort order
/// @param enabled     whether the item is enabled
/// @param extra       optional JSON payload
/// @param parentCode  value of the parent item
/// @param level       depth inside the tree
/// @param description optional description
public record DictItemRequest(
        @NotBlank @Size(max = 128) String label,
        @Size(max = 128) String value,
        Integer sortNo,
        Boolean enabled,
        @Size(max = 1024) String extra,
        @Size(max = 128) String parentCode,
        @Positive Integer level,
        @Size(max = 512) String description) {
}
