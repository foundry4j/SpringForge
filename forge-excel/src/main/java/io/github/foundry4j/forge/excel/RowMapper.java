package io.github.foundry4j.forge.excel;

/// Maps a raw [RowData] to a business object.
///
/// Any exception thrown here is captured and recorded as a row error, so one bad row
/// never aborts the whole import.
///
/// @param <T> mapped type
@FunctionalInterface
public interface RowMapper<T> {

    /// Maps one row.
    T map(RowData row) throws Exception;
}
