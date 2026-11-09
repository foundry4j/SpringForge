package io.github.foundry4j.forge.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A single import error returned with [ImportResult].
///
/// @param rowNumber row that failed
/// @param field     offending field, `null` for row level errors
/// @param value     raw cell value
/// @param message   human readable explanation
/// @param rowValues all raw values of the row, useful for client side preview
public record RowError(int rowNumber, String field, String value, String message, List<String> rowValues) {

    /// Copies the raw values so that the error is immutable.
    public RowError {
        rowValues = rowValues == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(rowValues));
    }
}
