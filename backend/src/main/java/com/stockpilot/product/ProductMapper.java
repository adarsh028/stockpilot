package com.stockpilot.product;

import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.product.dto.ProductResponse;
import com.stockpilot.product.dto.SkuResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ProductMapper {

    public SkuResponse toSkuResponse(Sku sku, InventoryItem inventory) {
        return new SkuResponse(
                sku.getId().toString(),
                sku.getProductId().toString(),
                sku.getSku(),
                sku.getAttributes(),
                sku.getCostPrice(),
                sku.getSellingPrice(),
                inventory != null ? inventory.getQuantityOnHand() : 0,
                inventory != null ? inventory.getReorderLevel() : 0
        );
    }

    public ProductResponse toResponse(Product product, List<Sku> skus, Map<UUID, InventoryItem> inventoryBySkuId) {
        List<SkuResponse> skuResponses = skus.stream()
                .map(sku -> toSkuResponse(sku, inventoryBySkuId.get(sku.getId())))
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
}
