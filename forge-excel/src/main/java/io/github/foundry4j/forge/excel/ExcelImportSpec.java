package io.github.foundry4j.forge.excel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/// Import configuration: row mapper, validators, success sink and error budget.
///
/// ```java
/// ExcelImportSpec<User> spec = ExcelImportSpec.<User>builder()
///         .mapper(row -> new User(row.cell("id"), row.cell("name")))
///         .validator((user, ctx) -> {
///             if (user.name().isBlank()) {
///                 ctx.reject("name", user.name(), "Name must not be blank");
///             }
///         })
///         .rowSink(users::add)
///         .build();
/// ```
///
/// @param sheetIndex     worksheet index
/// @param headerRowIndex header row index, `-1` when the file has no header
/// @param maxErrors      maximum retained errors, `0` means unlimited
/// @param skipEmptyRows  whether blank rows are skipped
/// @param charset        CSV encoding
/// @param mapper         mandatory row mapper
/// @param validators     row validators
/// @param rowSink        consumer of successful rows, `null` to only count them
/// @param <T>            mapped type
public record ExcelImportSpec<T>(
        int sheetIndex,
        int headerRowIndex,
        int maxErrors,
        boolean skipEmptyRows,
        Charset charset,
        RowMapper<T> mapper,
        List<RowValidator<T>> validators,
        Consumer<T> rowSink) {

    /// Validates the configuration and copies the mutable collections.
    public ExcelImportSpec {
        if (mapper == null) {
            throw new IllegalArgumentException("mapper must not be null");
        }
        if (sheetIndex < 0) {
            throw new IllegalArgumentException("sheetIndex must not be negative");
        }
        if (headerRowIndex < -1) {
            throw new IllegalArgumentException("headerRowIndex must be -1 or greater");
        }
        if (maxErrors < 0) {
            throw new IllegalArgumentException("maxErrors must not be negative");
        }
        charset = charset == null ? StandardCharsets.UTF_8 : charset;
        validators = validators == null ? List.of() : List.copyOf(validators);
    }

    /// Converts the specification into reader options.
    public RowReadOptions rowReadOptions() {
        return new RowReadOptions(sheetIndex, headerRowIndex, skipEmptyRows, charset);
    }

    /// Starts building an import specification.
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /// Fluent builder of [ExcelImportSpec].
    ///
    /// @param <T> mapped type
    public static final class Builder<T> {

        private int sheetIndex;
        private int headerRowIndex;
        private int maxErrors = 500;
        private boolean skipEmptyRows = true;
        private Charset charset = StandardCharsets.UTF_8;
        private RowMapper<T> mapper;
        private final List<RowValidator<T>> validators = new ArrayList<>();
        private Consumer<T> rowSink;

        /// Sets the worksheet index, defaults to 0.
        public Builder<T> sheet(int sheetIndex) {
            this.sheetIndex = sheetIndex;
            return this;
        }

        /// Sets the header row index, defaults to 0; `-1` disables headers.
        public Builder<T> headerRow(int headerRowIndex) {
            this.headerRowIndex = headerRowIndex;
            return this;
        }

        /// Sets the error cap, defaults to 500; `0` means unlimited.
        public Builder<T> maxErrors(int maxErrors) {
            this.maxErrors = maxErrors;
            return this;
        }

        /// Skips blank rows, enabled by default.
        public Builder<T> skipEmptyRows(boolean skipEmptyRows) {
            this.skipEmptyRows = skipEmptyRows;
            return this;
        }

        /// Sets the CSV charset, defaults to UTF-8.
        public Builder<T> charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /// Sets the mandatory row mapper.
        public Builder<T> mapper(RowMapper<T> mapper) {
            this.mapper = mapper;
            return this;
        }

        /// Adds a row validator.
        public Builder<T> validator(RowValidator<T> validator) {
            if (validator != null) {
                this.validators.add(validator);
            }
            return this;
        }

        /// Adds several row validators.
        public Builder<T> validators(Collection<RowValidator<T>> validators) {
            if (validators != null) {
                this.validators.addAll(validators);
            }
            return this;
        }

        /// Sets the consumer of successfully imported rows.
        public Builder<T> rowSink(Consumer<T> rowSink) {
            this.rowSink = rowSink;
            return this;
        }

        /// Creates the immutable specification.
        public ExcelImportSpec<T> build() {
            return new ExcelImportSpec<>(sheetIndex, headerRowIndex, maxErrors, skipEmptyRows,
                    charset, mapper, validators, rowSink);
        }
    }
}
