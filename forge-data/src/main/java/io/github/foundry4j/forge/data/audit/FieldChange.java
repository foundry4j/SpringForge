package io.github.foundry4j.forge.data.audit;

import java.io.Serial;
import java.io.Serializable;

/// A single field level change captured by the audit listener.
///
/// @param field    name of the changed property
/// @param oldValue value before the change, `null` for a newly set field
/// @param newValue value after the change, `null` for a cleared field
public record FieldChange(String field, String oldValue, String newValue) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /// Rejects blank field names so that audit records stay searchable.
    public FieldChange {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field must not be blank");
        }
    }
}
