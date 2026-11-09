package io.github.foundry4j.forge.excel;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.web.file.FileDownloads;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;

/// Spring MVC helpers: import an upload and stream a download.
///
/// ```java
/// @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
/// public ImportResult importUsers(@RequestPart("file") MultipartFile file) {
///     return ExcelHttp.importUpload(file, spec);
/// }
///
/// @GetMapping("/export")
/// public ResponseEntity<StreamingResponseBody> exportOrders() {
///     return ExcelHttp.download("orders.xlsx", spec, service.streamAll());
/// }
/// ```
public final class ExcelHttp {

    private ExcelHttp() {
    }

    /// Detects the format of an upload and stream-imports it.
    public static <T> ImportResult importUpload(MultipartFile upload, ExcelImportSpec<T> spec) {
        if (upload == null || upload.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing uploaded file");
        }
        try (InputStream in = upload.getInputStream()) {
            return ExcelImporter.importData(in, formatOf(upload), spec);
        } catch (IOException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Failed to read the uploaded file", ex);
        }
    }

    /// Detects the format of an upload.
    public static ExcelFormat formatOf(MultipartFile upload) {
        if (upload == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing uploaded file");
        }
        return ExcelFormat.detect(upload.getOriginalFilename(), upload.getContentType());
    }

    /// Streams a response whose body is produced by `writer`.
    public static ResponseEntity<StreamingResponseBody> download(String filename, ExcelFormat format,
                                                                 Consumer<OutputStream> writer) {
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing download file name");
        }
        StreamingResponseBody body = writer::accept;
        return ResponseEntity.ok()
                .contentType(mediaType(format))
                .header(HttpHeaders.CONTENT_DISPOSITION, FileDownloads.contentDisposition(filename, true))
                .body(body);
    }

    /// Exports `rows`; the format is implied by the file name suffix.
    public static <T> ResponseEntity<StreamingResponseBody> download(String filename, ExcelExportSpec<T> spec,
                                                                     Iterable<? extends T> rows) {
        ExcelFormat format = ExcelFormat.fromFilename(filename);
        return download(filename, format, out -> ExcelExporter.write(out, format, spec, rows));
    }

    private static MediaType mediaType(ExcelFormat format) {
        if (format == ExcelFormat.CSV) {
            return MediaType.parseMediaType("text/csv;charset=UTF-8");
        }
        return MediaType.parseMediaType(format.mediaType());
    }
}
