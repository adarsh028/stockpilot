package com.stockpilot.integration.drive;

import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.common.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.UUID;

/**
 * High-level image storage backed by the tenant's connected Google Drive. Callers deal
 * only in Drive file ids; token refresh, folder provisioning, and validation are handled
 * here. Uploads fail fast with a 409 when the org has not connected Drive yet.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveStorageService {

    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10MB, matches multipart config
    private static final Map<String, String> ALLOWED = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final DriveConnectionRepository connectionRepository;
    private final DriveTokenService tokenService;
    private final GoogleDriveClient driveClient;
    private final GoogleDriveProperties properties;

    public boolean isConnected(UUID orgId) {
        return connectionRepository.existsByOrganizationId(orgId);
    }

    /**
     * Validates and uploads an image into the org's Drive folder, returning the Drive
     * file id. Throws {@link ConflictException} if Drive is not connected.
     */
    @Transactional
    public String uploadImage(UUID orgId, MultipartFile file) {
        String extension = validate(file);
        DriveConnection connection = requireConnection(orgId);
        String accessToken = tokenService.validAccessToken(connection);
        String folderId = ensureFolder(connection, accessToken);

        try {
            String name = UUID.randomUUID() + extension;
            return driveClient.uploadFile(accessToken, folderId, name, file.getContentType(), file.getBytes());
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded image", ex);
        }
    }

    /** Streams a file's bytes back from Drive. */
    @Transactional
    public GoogleDriveClient.DriveContent download(UUID orgId, String fileId) {
        DriveConnection connection = requireConnection(orgId);
        String accessToken = tokenService.validAccessToken(connection);
        return driveClient.downloadFile(accessToken, fileId);
    }

    /** Best-effort delete; a failure here should not block removing the DB record. */
    @Transactional
    public void delete(UUID orgId, String fileId) {
        connectionRepository.findByOrganizationId(orgId).ifPresent(connection -> {
            try {
                String accessToken = tokenService.validAccessToken(connection);
                driveClient.deleteFile(accessToken, fileId);
            } catch (Exception ex) {
                log.warn("Could not delete Drive file {} for org {}: {}", fileId, orgId, ex.getMessage());
            }
        });
    }

    /** Lazily creates the product-images folder the first time it is needed. */
    private String ensureFolder(DriveConnection connection, String accessToken) {
        if (connection.getFolderId() != null && !connection.getFolderId().isBlank()) {
            return connection.getFolderId();
        }
        String folderId = driveClient.createFolder(accessToken, properties.getFolderName());
        connection.setFolderId(folderId);
        return folderId;
    }

    private DriveConnection requireConnection(UUID orgId) {
        return connectionRepository.findByOrganizationId(orgId)
                .orElseThrow(() -> new ConflictException(
                        "Google Drive is not connected. Connect it in Settings before adding images."));
    }

    private String validate(MultipartFile file) {
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
        return extension;
    }
}
