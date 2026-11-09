package io.github.foundry4j.forge.web.file;

import java.io.IOException;
import java.io.InputStream;

/// Service provider interface for binary file storage.
///
/// [LocalFileStorage] is the default implementation. Declaring a bean of this type
/// replaces it, so an application can plug an object store such as S3 or MinIO
/// without touching the endpoints.
///
/// Implementations must never build a path from the original file name; only the
/// generated key may reach the storage backend.
public interface FileStorage {

    /// Stores a file and returns its metadata.
    ///
    /// @param in               content stream, closed by the caller
    /// @param originalFilename original name, used only to derive the extension
    /// @param contentType      MIME type, may be `null`
    /// @throws io.github.foundry4j.forge.core.exception.BusinessException when the file is
    ///         too large or its media type is not accepted
    StoredFile store(InputStream in, String originalFilename, String contentType);

    /// Opens the content of a stored file, or returns `null` when the key is unknown.
    InputStream open(String key) throws IOException;

    /// Whether a key exists.
    boolean exists(String key);

    /// Deletes a key, silently doing nothing when it does not exist.
    void delete(String key);
}
