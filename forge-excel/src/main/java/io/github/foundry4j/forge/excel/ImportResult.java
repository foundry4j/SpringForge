package io.github.foundry4j.forge.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Outcome of an import: counters plus the failed rows.
///
/// The error list is capped by [ExcelImportSpec#maxErrors()]; when the cap is reached
/// `errorsTruncated` becomes `true` while scanning continues, so the counters stay exact.
///
/// @param totalRows       scanned data rows
/// @param successRows     rows that passed mapping and validation
/// @param failedRows      rows with at least one error
/// @param errors          error details, possibly truncated
/// @param errorsTruncated whether the error list was truncated
public record ImportResult(long totalRows, long successRows, long failedRows,
                           List<RowError> errors, boolean errorsTruncated) {

    /// Copies the errors so that the result is immutable.
    public ImportResult {
        errors = errors == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /// Whether at least one row failed.
    public boolean hasErrors() {
        return failedRows > 0;
    }
}
