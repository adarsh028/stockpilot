package com.stockpilot.product;

import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.integration.drive.GoogleDriveClient;
import com.stockpilot.integration.drive.GoogleDriveStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Streams product images stored in a tenant's Google Drive back to the browser. Public
 * (no JWT) because it is loaded via {@code <img src>}; authorization comes from the signed
 * token in the query string, which binds the request to a specific image + organization.
 */
@RestController
@RequiredArgsConstructor
public class ImageContentController {

    private final SkuImageRepository skuImageRepository;
    private final GoogleDriveStorageService driveStorageService;
    private final ImageUrlSigner imageUrlSigner;

    @GetMapping("/api/v1/images/{imageId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID imageId,
                                          @RequestParam("token") String token) {
        UUID orgId = imageUrlSigner.verify(token, imageId);

        SkuImage image = skuImageRepository.findByIdAndOrganizationId(imageId, orgId)
                .orElseThrow(() -> ResourceNotFoundException.of("Image", imageId));
        if (!image.getUrl().startsWith(SkuImage.GDRIVE_PREFIX)) {
            throw ResourceNotFoundException.of("Image", imageId);
        }
        String fileId = image.getUrl().substring(SkuImage.GDRIVE_PREFIX.length());

        GoogleDriveClient.DriveContent file = driveStorageService.download(orgId, fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(file.bytes());
    }
}
