package com.stockpilot.product;

import com.stockpilot.common.exception.ResourceNotFoundException;
import com.stockpilot.integration.drive.GoogleDriveStorageService;
import com.stockpilot.product.dto.SkuImageResponse;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkuImageService {

    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final GoogleDriveStorageService driveStorageService;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;
    private final ProductMapper mapper;

    @Transactional(readOnly = true)
    public List<SkuImageResponse> list(UUID skuId) {
        UUID orgId = currentTenant.organizationId();
        requireSku(skuId, orgId);
        return skuImageRepository
                .findBySkuIdAndOrganizationIdOrderByPrimaryDescSortOrderAscCreatedAtAsc(skuId, orgId)
                .stream()
                .map(mapper::toImageResponse)
                .toList();
    }

    @Transactional
    public SkuImageResponse upload(UUID skuId, MultipartFile file, boolean makePrimary) {
        UUID orgId = currentTenant.organizationId();
        requireSku(skuId, orgId);

        long existingCount = skuImageRepository.countBySkuIdAndOrganizationId(skuId, orgId);
        // The first image of a SKU is always primary; otherwise honour the flag.
        boolean primary = makePrimary || existingCount == 0;

        String fileId = driveStorageService.uploadImage(orgId, file);
        if (primary) {
            clearPrimary(skuId, orgId);
        }

        SkuImage image = new SkuImage();
        image.setOrganizationId(orgId);
        image.setSkuId(skuId);
        image.setUrl(SkuImage.GDRIVE_PREFIX + fileId);
        image.setPrimary(primary);
        image.setSortOrder((int) existingCount);
        image = skuImageRepository.saveAndFlush(image);
        return mapper.toImageResponse(image);
    }

    @Transactional
    public SkuImageResponse setPrimary(UUID skuId, UUID imageId) {
        UUID orgId = currentTenant.organizationId();
        requireSku(skuId, orgId);
        SkuImage image = requireImage(skuId, imageId, orgId);
        if (!image.isPrimary()) {
            clearPrimary(skuId, orgId);
            image.setPrimary(true);
            image = skuImageRepository.saveAndFlush(image);
        }
        return mapper.toImageResponse(image);
    }

    @Transactional
    public void delete(UUID skuId, UUID imageId) {
        UUID orgId = currentTenant.organizationId();
        requireSku(skuId, orgId);
        SkuImage image = requireImage(skuId, imageId, orgId);
        boolean wasPrimary = image.isPrimary();

        if (image.getUrl().startsWith(SkuImage.GDRIVE_PREFIX)) {
            String fileId = image.getUrl().substring(SkuImage.GDRIVE_PREFIX.length());
            driveStorageService.delete(orgId, fileId);
        }
        skuImageRepository.delete(image);
        skuImageRepository.flush();

        // Promote the next remaining image so a SKU with images always has a primary.
        if (wasPrimary) {
            skuImageRepository.findBySkuIdAndOrganizationId(skuId, orgId).stream()
                    .min(Comparator.comparingInt(SkuImage::getSortOrder)
                            .thenComparing(SkuImage::getCreatedAt))
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        skuImageRepository.save(next);
                    });
        }
    }

    /** Unsets the current primary (if any) and flushes so the unique index never conflicts. */
    private void clearPrimary(UUID skuId, UUID orgId) {
        List<SkuImage> current = skuImageRepository.findBySkuIdAndOrganizationId(skuId, orgId).stream()
                .filter(SkuImage::isPrimary)
                .toList();
        if (!current.isEmpty()) {
            current.forEach(img -> img.setPrimary(false));
            skuImageRepository.saveAll(current);
            skuImageRepository.flush();
        }
    }

    private void requireSku(UUID skuId, UUID orgId) {
        tenantGuard.loadOwned("SKU", skuId, orgId,
                () -> skuRepository.findByIdAndOrganizationId(skuId, orgId));
    }

    private SkuImage requireImage(UUID skuId, UUID imageId, UUID orgId) {
        SkuImage image = tenantGuard.loadOwned("Image", imageId, orgId,
                () -> skuImageRepository.findByIdAndOrganizationId(imageId, orgId));
        if (!image.getSkuId().equals(skuId)) {
            throw ResourceNotFoundException.of("Image", imageId);
        }
        return image;
    }
}
