package com.stockpilot.product;

import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.product.dto.ProductResponse;
import com.stockpilot.product.dto.SkuImageResponse;
import com.stockpilot.product.dto.SkuResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final ImageUrlSigner imageUrlSigner;

    public SkuImageResponse toImageResponse(SkuImage image) {
        return new SkuImageResponse(
                image.getId().toString(),
                image.getSkuId().toString(),
                resolveUrl(image),
                image.isPrimary(),
                image.getSortOrder()
        );
    }

    public SkuResponse toSkuResponse(Sku sku, InventoryItem inventory, List<SkuImage> images) {
        List<SkuImageResponse> imageResponses = (images == null ? List.<SkuImage>of() : images).stream()
                .map(this::toImageResponse)
                .toList();
        return new SkuResponse(
                sku.getId().toString(),
                sku.getProductId().toString(),
                sku.getSku(),
                sku.getAttributes(),
                sku.getCostPrice(),
                sku.getSellingPrice(),
                inventory != null ? inventory.getQuantityOnHand() : 0,
                inventory != null ? inventory.getReorderLevel() : 0,
                imageResponses
        );
    }

    public ProductResponse toResponse(Product product, List<Sku> skus,
                                      Map<UUID, InventoryItem> inventoryBySkuId,
                                      Map<UUID, List<SkuImage>> imagesBySkuId) {
        List<SkuResponse> skuResponses = skus.stream()
                .map(sku -> toSkuResponse(
                        sku,
                        inventoryBySkuId.get(sku.getId()),
                        imagesBySkuId.getOrDefault(sku.getId(), Collections.emptyList())))
                .toList();
        return new ProductResponse(
                product.getId().toString(),
                product.getName(),
                product.getCategory(),
                product.getBrandName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getStatus().name(),
                skuResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * Resolves an image's stored locator into a client-usable URL. Drive-backed images
     * are served through the signed proxy endpoint; legacy local paths are served directly.
     */
    private String resolveUrl(SkuImage image) {
        String stored = image.getUrl();
        if (stored != null && stored.startsWith(SkuImage.GDRIVE_PREFIX)) {
            String token = imageUrlSigner.issue(image.getOrganizationId(), image.getId());
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/images/" + image.getId() + "/content")
                    .queryParam("token", token)
                    .toUriString();
        }
        return absoluteUrl(stored);
    }

    /** Turns a stored relative path ({@code /uploads/x.jpg}) into a full URL for the client. */
    private String absoluteUrl(String storedUrl) {
        if (storedUrl == null || storedUrl.startsWith("http://") || storedUrl.startsWith("https://")) {
            return storedUrl;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(storedUrl)
                .toUriString();
    }
}
