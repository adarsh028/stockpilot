package com.stockpilot.integration.drive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Wrapper over the Google Drive v3 REST API for the handful of operations this app
 * needs: create a folder, upload a file into it, download a file's bytes, and delete.
 * All calls take a caller-supplied OAuth access token (see {@link DriveTokenService}).
 */
@Component
public class GoogleDriveClient {

    private static final String FILES_ENDPOINT = "https://www.googleapis.com/drive/v3/files";
    private static final String UPLOAD_ENDPOINT =
            "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart";
    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Creates a folder in the account root and returns its file id. */
    public String createFolder(String accessToken, String name) {
        Map<String, Object> metadata = Map.of("name", name, "mimeType", FOLDER_MIME);
        DriveFile file = restClient.post()
                .uri(FILES_ENDPOINT + "?fields=id")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(metadata)
                .retrieve()
                .body(DriveFile.class);
        return file == null ? null : file.id();
    }

    /**
     * Uploads bytes into {@code folderId} using a single multipart/related request
     * (metadata part + media part). Returns the new file id.
     */
    public String uploadFile(String accessToken, String folderId, String name,
                             String contentType, byte[] content) {
        String boundary = "stockpilot" + Integer.toHexString(name.hashCode()) + content.length;
        byte[] body = buildMultipartRelated(boundary, name, folderId, contentType, content);

        DriveFile file = restClient.post()
                .uri(UPLOAD_ENDPOINT + "&fields=id")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .contentType(MediaType.parseMediaType("multipart/related; boundary=" + boundary))
                .body(body)
                .retrieve()
                .body(DriveFile.class);
        return file == null ? null : file.id();
    }

    /** Downloads a file's raw bytes plus the content type Drive reports for it. */
    public DriveContent downloadFile(String accessToken, String fileId) {
        ResponseEntity<byte[]> response = restClient.get()
                .uri(FILES_ENDPOINT + "/" + fileId + "?alt=media")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .toEntity(byte[].class);
        MediaType type = response.getHeaders().getContentType();
        return new DriveContent(
                response.getBody(),
                type != null ? type.toString() : MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /** Deletes a file. */
    public void deleteFile(String accessToken, String fileId) {
        restClient.delete()
                .uri(FILES_ENDPOINT + "/" + fileId)
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                .retrieve()
                .toBodilessEntity();
    }

    private byte[] buildMultipartRelated(String boundary, String name, String folderId,
                                         String contentType, byte[] content) {
        try {
            String metadataJson = objectMapper.writeValueAsString(
                    Map.of("name", name, "parents", List.of(folderId)));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(metadataJson.getBytes(StandardCharsets.UTF_8));
            out.write(("\r\n--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(content);
            out.write(("\r\n--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to build Drive upload body", ex);
        }
    }

    private static String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    public record DriveContent(byte[] bytes, String contentType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DriveFile(String id) {
    }
}
