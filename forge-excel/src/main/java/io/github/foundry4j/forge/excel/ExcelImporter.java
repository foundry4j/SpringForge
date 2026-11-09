package io.github.foundry4j.forge.excel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/// Streams an import: map, validate and collect errors row by row.
///
/// A mapping or validation failure never aborts the file; failed rows are returned as
/// [RowError] while successful rows are handed to [ExcelImportSpec#rowSink()].
///
/// ```java
/// List<User> users = new ArrayList<>();
/// ImportResult result = ExcelImporter.importData(in, ExcelFormat.XLSX,
///         ExcelImportSpec.<User>builder()
///                 .mapper(row -> new User(row.cell("id"), row.cell("name")))
///                 .validator((user, ctx) -> {
///                     if (user.name().isBlank()) {
///                         ctx.reject("name", user.name(), "Name must not be blank");
///                     }
///                 })
///                 .rowSink(users::add)
///                 .build());
/// ```
public final class ExcelImporter {

    private ExcelImporter() {
    }

    /// Imports a file using the format implied by the file name suffix.
    public static <T> ImportResult importData(InputStream in, String filename, ExcelImportSpec<T> spec) {
        return importData(in, ExcelFormat.fromFilename(filename), spec);
    }

    /// Streams an import and returns counters plus error details.
    public static <T> ImportResult importData(InputStream in, ExcelFormat format, ExcelImportSpec<T> spec) {
        if (spec == null) {
            throw new IllegalArgumentException("spec must not be null");
        }

        long[] counters = new long[2];
        long[] failed = new long[1];
        List<RowError> errors = new ArrayList<>();
        boolean[] truncated = new boolean[1];

        ExcelReader.read(in, format, spec.rowReadOptions(), row -> {
            counters[0]++;
            RowContext context = new RowContext(row);
            T value = null;

            try {
                value = spec.mapper().map(row);
                if (value == null && !context.hasErrors()) {
                    context.reject("Mapping produced no value");
                }
            } catch (Exception ex) {
                context.reject("Mapping failed: " + message(ex));
            }

            if (value != null && !context.hasErrors()) {
                for (RowValidator<T> validator : spec.validators()) {
                    try {
                        validator.validate(value, context);
                    } catch (Exception ex) {
                        context.reject("Validation failed: " + message(ex));
                    }
                }
            }

            if (context.hasErrors()) {
                failed[0]++;
                addErrors(errors, context.errors(), spec.maxErrors(), truncated);
            } else {
                counters[1]++;
                if (spec.rowSink() != null) {
                    spec.rowSink().accept(value);
                }
            }
        });

        return new ImportResult(counters[0], counters[1], failed[0], errors, truncated[0]);
    }

    private static void addErrors(List<RowError> target, List<RowError> rowErrors,
                                  int maxErrors, boolean[] truncated) {
        for (RowError error : rowErrors) {
            if (maxErrors > 0 && target.size() >= maxErrors) {
                truncated[0] = true;
                return;
            }
            target.add(error);
        }
    }

    private static String message(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}
