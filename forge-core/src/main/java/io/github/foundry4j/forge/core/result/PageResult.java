package io.github.foundry4j.forge.core.result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/// Framework neutral page envelope.
///
/// The data module converts Spring Data pages into this record so that the web
/// layer never leaks `Page` implementation details to clients.
///
/// @param records page content
/// @param total   total number of matching rows
/// @param page    zero based page index
/// @param size    requested page size
/// @param pages   total number of pages
public record PageResult<T>(
        List<T> records,
        long total,
        int page,
        int size,
        int pages
) {

    /// Normalizes the content and derives the page count from total and size.
    public PageResult {
        records = records == null ? List.of() : List.copyOf(records);
    }

    /// Creates a page result and computes the page count.
    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        int pages = size <= 0 ? 0 : (int) Math.ceilDiv(total, size);
        return new PageResult<>(records, total, page, size, pages);
    }

    /// Creates an empty page.
    public static <T> PageResult<T> empty(int page, int size) {
        return new PageResult<>(List.of(), 0L, page, size, 0);
    }

    /// Transforms the content while preserving pagination metadata.
    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = new ArrayList<>(records.size());
        for (T record : records) {
            mapped.add(mapper.apply(record));
        }
        return new PageResult<>(mapped, total, page, size, pages);
    }
}
