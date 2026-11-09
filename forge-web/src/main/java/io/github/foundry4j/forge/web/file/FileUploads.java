package io.github.foundry4j.forge.web.file;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/// Helper that streams a Spring [MultipartFile] into a [FileStorage].
///
/// ```java
/// @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
/// public StoredFile upload(@RequestPart("file") MultipartFile file) {
///     return FileUploads.store(storage, file);
/// }
/// ```
public final class FileUploads {

    private FileUploads() {
    }

    /// Stores the uploaded file.
    public static StoredFile store(FileStorage storage, MultipartFile upload) {
        if (upload == null || upload.isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Missing uploaded file");
        }
        try (InputStream in = upload.getInputStream()) {
            return storage.store(in, upload.getOriginalFilename(), upload.getContentType());
        } catch (IOException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "Failed to read uploaded file", ex);
        }
    }
}
