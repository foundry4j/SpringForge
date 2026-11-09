package io.github.foundry4j.forge.app.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// Request body of dictionary type create and update calls.
///
/// @param code    unique code, may be omitted on update
/// @param name    display name
/// @param remark  optional remark
/// @param enabled whether the dictionary is enabled
public record DictTypeRequest(
        @Size(max = 64) String code,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String remark,
        Boolean enabled) {
}
