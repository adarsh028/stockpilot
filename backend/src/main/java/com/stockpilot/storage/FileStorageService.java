package com.stockpilot.storage;

import com.stockpilot.common.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stores uploaded images on the local filesystem and hands back a relative URL
 * ({@code /uploads/<name>}) that the static resource handler serves. Isolated behind
 * this class so it can be swapped for object storage (S3/GCS) later without touching
 * callers. Mirrors the disk-based approach used for import error reports.
 */
@Slf4j
@Component
public class FileStorageService {

    /** Public URL prefix; also the static resource mapping in WebConfig. */
    public static final String PUBLIC_PREFIX = "/uploads/";

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10MB, matches multipart config
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final Path baseDir;

    public FileStorageService(@Value("${app.storage.uploads-dir}") String dir) {
        this.baseDir = Paths.get(dir).toAbsolutePath();
    }

    /**
     * Persists an uploaded image and returns its relative public URL. Rejects empty,
     * oversized, or non-image files.
     */
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("Image file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ValidationException("Image exceeds the 10MB limit");
        }
        String contentType = file.getContentType();
        String extension = contentType == null ? null : ALLOWED.get(contentType.toLowerCase());
        if (extension == null) {
            throw new ValidationException("Unsupported image type. Allowed: JPEG, PNG, WEBP, GIF");
        }
        try {
            Files.createDirectories(baseDir);
            String fileName = UUID.randomUUID() + extension;
            Path target = baseDir.resolve(fileName);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return PUBLIC_PREFIX + fileName;
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to store uploaded image", ex);
        }
    }

    /** Best-effort deletion of a previously stored file given its public URL. */
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith(PUBLIC_PREFIX)) {
            return;
        }
        String fileName = publicUrl.substring(PUBLIC_PREFIX.length());
        // Guard against path traversal — only a bare file name is ever expected here.
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            return;
        }
        try {
            Files.deleteIfExists(baseDir.resolve(fileName));
        } catch (IOException ex) {
            log.warn("Could not delete image file {}: {}", fileName, ex.getMessage());
        }
    }
}
