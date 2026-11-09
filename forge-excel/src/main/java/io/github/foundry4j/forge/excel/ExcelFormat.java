package io.github.foundry4j.forge.excel;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;

import java.util.Locale;

/// Supported spreadsheet formats.
///
/// Only OOXML (`.xlsx` / `.xlsm`) and CSV are handled; the legacy `.xls` format must be
/// converted before it can be imported.
public enum ExcelFormat {

    /// Office Open XML workbook.
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),

    /// Comma separated values.
    CSV("text/csv");

    private final String mediaType;

    ExcelFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    /// Media type used when the file is streamed to a client.
    public String mediaType() {
        return mediaType;
    }

    /// Resolves the format from a file name suffix.
    ///
    /// @throws BusinessException when the suffix is not supported
    public static ExcelFormat fromFilename(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".xlsx") || lower.endsWith(".xlsm")) {
            return XLSX;
        }
        if (lower.endsWith(".csv")) {
            return CSV;
        }
        throw new BusinessException(CommonErrorCode.BAD_REQUEST,
                "Unsupported file type, only .xlsx/.xlsm/.csv are accepted");
    }

    /// Detects the format from the content type first and falls back to the file name.
    public static ExcelFormat detect(String filename, String contentType) {
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("csv")) {
            return CSV;
        }
        if (type.contains("spreadsheetml") || type.contains("ms-excel")) {
            return XLSX;
        }
        return fromFilename(filename);
    }
}
