package io.github.foundry4j.forge.excel;

/// Validates a mapped row and reports problems through [RowContext].
///
/// Thrown exceptions are converted into row errors by the importer.
///
/// @param <T> mapped type
@FunctionalInterface
public interface RowValidator<T> {

    /// Validates one mapped row.
    void validate(T value, RowContext context) throws Exception;
}
