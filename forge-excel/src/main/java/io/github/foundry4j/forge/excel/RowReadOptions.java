package io.github.foundry4j.forge.excel;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/// Options controlling how a file is streamed.
///
/// @param sheetIndex     zero based worksheet index, ignored for CSV
/// @param headerRowIndex zero based header row, `-1` when the file has no header
/// @param skipEmptyRows  whether rows whose cells are all blank are skipped
/// @param charset        CSV encoding, defaults to UTF-8
public record RowReadOptions(int sheetIndex, int headerRowIndex, boolean skipEmptyRows, Charset charset) {

    /// Validates the indexes and applies the default charset.
    public RowReadOptions {
        if (sheetIndex < 0) {
            throw new IllegalArgumentException("sheetIndex must not be negative");
        }
        if (headerRowIndex < -1) {
            throw new IllegalArgumentException("headerRowIndex must be -1 or greater");
        }
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
    }

    /// First worksheet, first row is the header, empty rows skipped, UTF-8.
    public static RowReadOptions defaults() {
        return new RowReadOptions(0, 0, true, StandardCharsets.UTF_8);
    }
}
