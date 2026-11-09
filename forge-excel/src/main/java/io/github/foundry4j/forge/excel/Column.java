package io.github.foundry4j.forge.excel;

import java.util.function.Function;

/// Declares one exported column: header, value extractor and suggested width.
///
/// ```java
/// Column.of("Order no", Order::orderNo)
/// ```
///
/// @param header column title
/// @param getter extracts the cell value from a row
/// @param width  suggested width in characters
/// @param <T>    row type
public record Column<T>(String header, Function<T, Object> getter, int width) {

    /// Validates the mandatory getter and normalizes optional values.
    public Column {
        if (getter == null) {
            throw new IllegalArgumentException("getter must not be null");
        }
        if (header == null) {
            header = "";
        }
        if (width <= 0) {
            width = 20;
        }
    }

    /// Creates a column with the default width.
    public Column(String header, Function<T, Object> getter) {
        this(header, getter, 20);
    }

    /// Creates a column with the default width.
    public static <T> Column<T> of(String header, Function<T, Object> getter) {
        return new Column<>(header, getter, 20);
    }

    /// Creates a column with an explicit width.
    public static <T> Column<T> of(String header, Function<T, Object> getter, int width) {
        return new Column<>(header, getter, width);
    }
}
