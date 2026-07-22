package com.stockpilot.inventory;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.inventory.dto.AdjustStockRequest;
import com.stockpilot.inventory.dto.InventoryItemResponse;
import com.stockpilot.product.Product;
import com.stockpilot.product.ProductRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final ChannelListingRepository channelListingRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final CurrentTenant currentTenant;

    @Transactional(readOnly = true)
    public PageResponse<InventoryItemResponse> list(Pageable pageable) {
        UUID orgId = currentTenant.organizationId();
        Page<InventoryItem> page = inventoryItemRepository.findByOrganizationId(orgId, pageable);
        return PageResponse.from(page, item -> enrich(orgId, item));
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> lowStock() {
        UUID orgId = currentTenant.organizationId();
        return inventoryItemRepository.findLowStock(orgId).stream()
                .map(item -> enrich(orgId, item))
                .toList();
    }

    @Transactional
    public InventoryItemResponse adjust(AdjustStockRequest req) {
        UUID orgId = currentTenant.organizationId();
        Sku sku = skuRepository.findByIdAndOrganizationId(req.skuId(), orgId)
                .orElseThrow(() -> new ValidationException("Unknown SKU"));

        InventoryItem item = inventoryItemRepository.findForUpdate(orgId, sku.getId())
                .orElseThrow(() -> new ValidationException("No inventory record for this SKU"));

        int before = item.getQuantityOnHand();
        int after;
        if (req.newQuantity() != null) {
            after = req.newQuantity();
        } else if (req.delta() != null) {
            after = before + req.delta();
        } else {
            throw new ValidationException("Provide either 'delta' or 'newQuantity'");
        }
        if (after < 0) {
            throw new ValidationException("Resulting quantity cannot be negative");
        }
        item.setQuantityOnHand(after);
        inventoryItemRepository.save(item);

        stockMovementService.record(orgId, sku.getId(), null, StockMovementType.ADJUSTMENT,
                after - before, req.reason() != null ? req.reason() : "Manual adjustment",
                null, currentTenant.userId());

        return enrich(orgId, item);
    }

    private InventoryItemResponse enrich(UUID orgId, InventoryItem item) {
        Sku sku = skuRepository.findByIdAndOrganizationId(item.getSkuId(), orgId).orElse(null);
        String skuCode = sku != null ? sku.getSku() : "";
        String productId = "";
        String productName = "";
        if (sku != null) {
            Product product = productRepository.findByIdAndOrganizationId(sku.getProductId(), orgId).orElse(null);
            if (product != null) {
                productId = product.getId().toString();
                productName = product.getName();
            }
        }
        int totalAllocated = channelListingRepository.findByOrganizationIdAndSkuId(orgId, item.getSkuId())
                .stream().mapToInt(ChannelListing::getAllocatedQuantity).sum();

        return new InventoryItemResponse(
                item.getId().toString(),
                item.getSkuId().toString(),
                skuCode,
                productId,
                productName,
                item.getQuantityOnHand(),
                item.getReorderLevel(),
                totalAllocated,
                item.getQuantityOnHand() <= item.getReorderLevel()
        );
    }

    /** Shared helper used by seeders/tests to build a sku-id → sku map. */
    @Transactional(readOnly = true)
    public Map<UUID, Sku> skuIndex(UUID orgId) {
        return skuRepository.findByOrganizationId(orgId).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
    }
}
