package com.stockpilot.product;

import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.inventory.InventoryItemRepository;
import com.stockpilot.product.dto.ProductRequest;
import com.stockpilot.product.dto.ProductResponse;
import com.stockpilot.product.dto.SkuRequest;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final SkuImageRepository skuImageRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;
    private final ProductMapper mapper;
    private final com.stockpilot.integration.drive.GoogleDriveStorageService driveStorageService;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> list(String search, String category, String status, Pageable pageable) {
        UUID orgId = currentTenant.organizationId();
        Specification<Product> spec = Specification.where(ProductSpecifications.ownedBy(orgId))
                .and(ProductSpecifications.search(search))
                .and(ProductSpecifications.hasCategory(category))
                .and(ProductSpecifications.hasStatus(status));
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::withDetails);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(UUID id) {
        UUID orgId = currentTenant.organizationId();
        Product product = tenantGuard.loadOwned("Product", id, orgId,
                () -> productRepository.findByIdAndOrganizationId(id, orgId));
        return withDetails(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        UUID orgId = currentTenant.organizationId();

        Product product = new Product();
        product.setOrganizationId(orgId);
        applyProductFields(product, req);
        product = productRepository.save(product);

        for (SkuRequest skuReq : req.skus()) {
            createSku(orgId, product.getId(), skuReq);
        }
        return withDetails(product);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        UUID orgId = currentTenant.organizationId();
        Product product = tenantGuard.loadOwned("Product", id, orgId,
                () -> productRepository.findByIdAndOrganizationId(id, orgId));
        applyProductFields(product, req);
        productRepository.save(product);

        List<Sku> existing = skuRepository.findByProductIdAndOrganizationId(product.getId(), orgId);
        Map<UUID, Sku> existingById = existing.stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));

        for (SkuRequest skuReq : req.skus()) {
            if (skuReq.id() != null && !skuReq.id().isBlank()) {
                UUID skuId = UUID.fromString(skuReq.id());
                Sku sku = existingById.remove(skuId);
                if (sku == null) {
                    throw new ValidationException("SKU does not belong to this product: " + skuReq.id());
                }
                updateSku(orgId, sku, skuReq);
            } else {
                createSku(orgId, product.getId(), skuReq);
            }
        }
        // Remove SKUs no longer present in the payload.
        for (Sku removed : existingById.values()) {
            deleteSkuImages(orgId, removed.getId());
            inventoryItemRepository.findByOrganizationIdAndSkuId(orgId, removed.getId())
                    .ifPresent(inventoryItemRepository::delete);
            skuRepository.delete(removed);
        }
        return withDetails(product);
    }

    @Transactional
    public void delete(UUID id) {
        UUID orgId = currentTenant.organizationId();
        Product product = tenantGuard.loadOwned("Product", id, orgId,
                () -> productRepository.findByIdAndOrganizationId(id, orgId));
        List<Sku> skus = skuRepository.findByProductIdAndOrganizationId(product.getId(), orgId);
        for (Sku sku : skus) {
            deleteSkuImages(orgId, sku.getId());
            inventoryItemRepository.findByOrganizationIdAndSkuId(orgId, sku.getId())
                    .ifPresent(inventoryItemRepository::delete);
        }
        skuRepository.deleteAll(skus);
        productRepository.delete(product);
    }

    private void applyProductFields(Product product, ProductRequest req) {
        product.setName(req.name());
        product.setCategory(req.category());
        product.setBrandName(req.brandName());
        product.setDescription(req.description());
        product.setImageUrl(req.imageUrl());
        if (req.status() != null && !req.status().isBlank()) {
            product.setStatus(ProductStatus.valueOf(req.status().toUpperCase()));
        }
    }

    private void createSku(UUID orgId, UUID productId, SkuRequest req) {
        if (skuRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, req.sku())) {
            throw new ConflictException("SKU code already exists: " + req.sku());
        }
        Sku sku = new Sku();
        sku.setOrganizationId(orgId);
        sku.setProductId(productId);
        sku.setSku(req.sku());
        sku.setAttributes(req.attributes() != null ? req.attributes() : new HashMap<>());
        sku.setCostPrice(req.costPrice() != null ? req.costPrice() : BigDecimal.ZERO);
        sku.setSellingPrice(req.sellingPrice() != null ? req.sellingPrice() : BigDecimal.ZERO);
        sku = skuRepository.save(sku);

        InventoryItem item = new InventoryItem();
        item.setOrganizationId(orgId);
        item.setSkuId(sku.getId());
        item.setQuantityOnHand(req.quantityOnHand() != null ? req.quantityOnHand() : 0);
        item.setReorderLevel(req.reorderLevel() != null ? req.reorderLevel() : 0);
        inventoryItemRepository.save(item);
    }

    private void updateSku(UUID orgId, Sku sku, SkuRequest req) {
        if (!sku.getSku().equalsIgnoreCase(req.sku())
                && skuRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, req.sku())) {
            throw new ConflictException("SKU code already exists: " + req.sku());
        }
        sku.setSku(req.sku());
        sku.setAttributes(req.attributes() != null ? req.attributes() : new HashMap<>());
        sku.setCostPrice(req.costPrice() != null ? req.costPrice() : BigDecimal.ZERO);
        sku.setSellingPrice(req.sellingPrice() != null ? req.sellingPrice() : BigDecimal.ZERO);
        skuRepository.save(sku);

        InventoryItem item = inventoryItemRepository.findByOrganizationIdAndSkuId(orgId, sku.getId())
                .orElseGet(() -> {
                    InventoryItem i = new InventoryItem();
                    i.setOrganizationId(orgId);
                    i.setSkuId(sku.getId());
                    return i;
                });
        if (req.quantityOnHand() != null) {
            item.setQuantityOnHand(req.quantityOnHand());
        }
        if (req.reorderLevel() != null) {
            item.setReorderLevel(req.reorderLevel());
        }
        inventoryItemRepository.save(item);
    }

    /** Deletes a SKU's image rows and their backing Drive files (best-effort per file). */
    private void deleteSkuImages(UUID orgId, UUID skuId) {
        List<SkuImage> images = skuImageRepository.findBySkuIdAndOrganizationId(skuId, orgId);
        for (SkuImage image : images) {
            if (image.getUrl() != null && image.getUrl().startsWith(SkuImage.GDRIVE_PREFIX)) {
                String fileId = image.getUrl().substring(SkuImage.GDRIVE_PREFIX.length());
                driveStorageService.delete(orgId, fileId);
            }
        }
        skuImageRepository.deleteAll(images);
    }

    private ProductResponse withDetails(Product product) {
        UUID orgId = product.getOrganizationId();
        List<Sku> skus = skuRepository.findByProductIdAndOrganizationId(product.getId(), orgId);
        Map<UUID, InventoryItem> inventoryBySku = new HashMap<>();
        Map<UUID, List<SkuImage>> imagesBySku = new HashMap<>();
        for (Sku sku : skus) {
            inventoryItemRepository.findByOrganizationIdAndSkuId(orgId, sku.getId())
                    .ifPresent(item -> inventoryBySku.put(sku.getId(), item));
            imagesBySku.put(sku.getId(),
                    skuImageRepository.findBySkuIdAndOrganizationIdOrderByPrimaryDescSortOrderAscCreatedAtAsc(
                            sku.getId(), orgId));
        }
        return mapper.toResponse(product, skus, inventoryBySku, imagesBySku);
    }
}
