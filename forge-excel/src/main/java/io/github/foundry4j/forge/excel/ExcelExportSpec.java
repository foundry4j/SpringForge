package io.github.foundry4j.forge.excel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/// Export configuration: sheet name, columns and streaming parameters.
///
/// ```java
/// ExcelExportSpec<User> spec = ExcelExportSpec.<User>builder("Users")
///         .column("ID", User::id, 12)
///         .column("Name", User::name, 24)
///         .build();
/// ExcelExporter.write(out, ExcelFormat.XLSX, spec, users);
/// ```
///
/// @param sheetName    worksheet name
/// @param columns      column definitions
/// @param windowSize   rows kept in memory before SXSSF flushes to disk
/// @param freezeHeader whether the header row stays visible while scrolling
/// @param csvBom       whether to write a UTF-8 BOM, needed by Excel to read Chinese CSV
/// @param <T>          row type
public record ExcelExportSpec<T>(String sheetName, List<Column<T>> columns,
                                 int windowSize, boolean freezeHeader, boolean csvBom) {

    /// Copies the columns and applies defaults.
    public ExcelExportSpec {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("columns must not be empty");
        }
        columns = List.copyOf(columns);
        if (sheetName == null || sheetName.isBlank()) {
            sheetName = "Sheet1";
        }
        if (windowSize <= 0) {
            windowSize = 100;
        }
    }

    /// Starts building an export specification.
    public static <T> Builder<T> builder(String sheetName) {
        return new Builder<>(sheetName);
    }

    /// Fluent builder of [ExcelExportSpec].
    ///
    /// @param <T> row type
    public static final class Builder<T> {

        private final String sheetName;
        private final List<Column<T>> columns = new ArrayList<>();
        private int windowSize = 100;
        private boolean freezeHeader = true;
        private boolean csvBom = true;

        private Builder(String sheetName) {
            this.sheetName = sheetName;
        }

        /// Adds a column with the default width.
        public Builder<T> column(String header, Function<T, Object> getter) {
            columns.add(Column.of(header, getter));
            return this;
        }

        /// Adds a column with an explicit width.
        public Builder<T> column(String header, Function<T, Object> getter, int width) {
            columns.add(Column.of(header, getter, width));
            return this;
        }

        /// Adds a pre-built column.
        public Builder<T> column(Column<T> column) {
            columns.add(column);
            return this;
        }

        /// Sets the SXSSF window size, defaults to 100 rows.
        public Builder<T> windowSize(int windowSize) {
            this.windowSize = windowSize;
            return this;
        }

        /// Freezes the header row, enabled by default.
        public Builder<T> freezeHeader(boolean freezeHeader) {
            this.freezeHeader = freezeHeader;
            return this;
        }

        /// Writes a UTF-8 BOM in CSV exports, enabled by default.
        public Builder<T> csvBom(boolean csvBom) {
            this.csvBom = csvBom;
            return this;
        }

        /// Creates the immutable specification.
        public ExcelExportSpec<T> build() {
            return new ExcelExportSpec<>(sheetName, columns, windowSize, freezeHeader, csvBom);
        }
    }
}
