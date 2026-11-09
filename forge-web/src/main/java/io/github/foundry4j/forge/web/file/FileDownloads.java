package io.github.foundry4j.forge.web.file;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/// Streams stored files to the client.
///
/// The content is never loaded fully into memory and the `Content-Disposition`
/// header is generated according to RFC 5987, so Chinese file names survive the trip
/// to browsers that do not understand bare UTF-8 names.
public final class FileDownloads {

    private FileDownloads() {
    }

    /// Returns the file as a download attachment.
    public static ResponseEntity<Resource> download(FileStorage storage, String key,
                                                    String filename, String contentType) {
        return build(storage, key, filename, contentType, true);
    }

    /// Returns the file inline, which lets browsers preview images and PDFs.
    public static ResponseEntity<Resource> inline(FileStorage storage, String key,
                                                  String filename, String contentType) {
        return build(storage, key, filename, contentType, false);
    }

    /// Builds a compatible `Content-Disposition` header value.
    public static String contentDisposition(String filename, boolean attachment) {
        String name = filename == null || filename.isBlank() ? "download" : filename;
        return (attachment ? "attachment" : "inline")
                + "; filename=\"" + asciiFallback(name) + "\""
                + "; filename*=UTF-8''" + encode(name);
    }

    private static ResponseEntity<Resource> build(FileStorage storage, String key, String filename,
                                                  String contentType, boolean attachment) {
        InputStream in;
        try {
            in = storage.open(key);
        } catch (IOException ex) {
            throw new BusinessException(CommonErrorCode.INTERNAL_ERROR, "Failed to read file", ex);
        }
        if (in == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "File not found");
        }
        return ResponseEntity.ok()
                .contentType(mediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(filename, attachment))
                .body(new InputStreamResource(in));
    }

    private static MediaType mediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException _) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static String asciiFallback(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            sb.append(c < 0x20 || c > 0x7E || c == '"' ? '_' : c);
        }
        return sb.toString();
    }

    private static String encode(String value) {
        StringBuilder sb = new StringBuilder(value.length() * 3);
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            char c = (char) (b & 0xFF);
            if (isUnreserved(c)) {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", b & 0xFF));
            }
        }
        return sb.toString();
    }

    private static boolean isUnreserved(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~';
    }
}
