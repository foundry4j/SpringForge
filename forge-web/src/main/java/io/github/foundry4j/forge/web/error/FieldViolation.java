package io.github.foundry4j.forge.web.error;

/// Single field level validation failure exposed in problem details.
///
/// @param field         name of the invalid field or parameter
/// @param message       violated constraint message
/// @param rejectedValue value that failed validation, may be `null`
public record FieldViolation(String field, String message, Object rejectedValue) {
}
