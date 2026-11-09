package io.github.foundry4j.forge.excel;

import java.util.ArrayList;
import java.util.List;

/// Per-row import context used by validators to reject a row without aborting the file.
///
/// ```java
/// (user, ctx) -> {
///     if (user.name().isBlank()) {
///         ctx.reject("name", ctx.row().cell("name"), "Name must not be blank");
///     }
/// }
/// ```
public final class RowContext {

    private final RowData row;
    private final List<RowError> errors = new ArrayList<>();

    /// Creates a context for one row.
    public RowContext(RowData row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        this.row = row;
    }

    /// Row being validated.
    public RowData row() {
        return row;
    }

    /// One based row number.
    public int rowNumber() {
        return row.rowNumber();
    }

    /// Records a field level error.
    public void reject(String field, String value, String message) {
        errors.add(new RowError(row.rowNumber(), field, value, message, row.values()));
    }

    /// Records a row level error.
    public void reject(String message) {
        reject(null, null, message);
    }

    /// Whether the row already has errors.
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /// Snapshot of the errors recorded so far.
    public List<RowError> errors() {
        return List.copyOf(errors);
    }
}
