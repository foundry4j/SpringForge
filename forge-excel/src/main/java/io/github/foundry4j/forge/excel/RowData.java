package io.github.foundry4j.forge.excel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// One read-only row produced by the streaming reader.
///
/// Strings are used for every cell so that callers decide how to parse numbers and
/// dates, which keeps import errors explicit instead of silently coercing values.
///
/// @param rowNumber one based row number, the physical row for Excel and the logical
///                  record index for CSV
/// @param headers   header names, empty when the file has no header row
/// @param values    cell text values, missing cells are empty strings
public record RowData(int rowNumber, List<String> headers, List<String> values) {

    /// Copies the lists so that a row is immutable.
    public RowData {
        headers = headers == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(headers));
        values = values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
    }

    /// Returns the cell at `index`, or an empty string when out of range.
    public String cell(int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value;
    }

    /// Returns the cell of a named header, or an empty string when unknown.
    public String cell(String header) {
        int index = headers.indexOf(header);
        return index < 0 ? "" : cell(index);
    }

    /// Number of cells in the row.
    public int size() {
        return values.size();
    }

    /// Whether every cell is blank.
    public boolean isEmpty() {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
