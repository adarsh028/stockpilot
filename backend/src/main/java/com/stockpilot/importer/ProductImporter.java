package com.stockpilot.importer;

import com.stockpilot.category.Category;
import com.stockpilot.category.CategoryRepository;
import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.inventory.InventoryItemRepository;
import com.stockpilot.product.Product;
import com.stockpilot.product.ProductRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;

/**
 * Persists a single product-import row in its OWN transaction so one bad row never
 * rolls back rows that already succeeded. Upsert semantics keyed on SKU code.
 */
@Service
@RequiredArgsConstructor
public class ProductImporter {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void importRow(UUID orgId, ImportRow row) {
        String name = row.get("name");
        String skuCode = row.get("sku");
        if (isBlank(name)) {
            throw new RowValidationException("Missing required column: name");
        }
        if (isBlank(skuCode)) {
            throw new RowValidationException("Missing required column: sku");
        }

        BigDecimal costPrice = parseMoney(row.get("costprice"), "costPrice");
        BigDecimal sellingPrice = parseMoney(row.get("sellingprice"), "sellingPrice");
        int quantity = parseInt(row.get("quantityonhand"), "quantityOnHand", 0);
        int reorder = parseInt(row.get("reorderlevel"), "reorderLevel", 0);

        Sku sku = skuRepository.findByOrganizationIdAndSkuIgnoreCase(orgId, skuCode).orElse(null);
        if (sku == null) {
            Product product = productRepository
                    .findFirstByOrganizationIdAndNameIgnoreCase(orgId, name)
                    .orElseGet(() -> {
                        Product p = new Product();
                        p.setOrganizationId(orgId);
                        p.setName(name);
                        p.setCategoryId(resolveCategoryId(orgId, row.get("category")));
                        p.setBrandName(row.get("brandname"));
                        return productRepository.save(p);
                    });

            sku = new Sku();
            sku.setOrganizationId(orgId);
            sku.setProductId(product.getId());
            sku.setSku(skuCode);
            sku.setAttributes(new HashMap<>());
            sku.setCostPrice(costPrice);
            sku.setSellingPrice(sellingPrice);
            sku = skuRepository.save(sku);

            InventoryItem item = new InventoryItem();
            item.setOrganizationId(orgId);
            item.setSkuId(sku.getId());
            item.setQuantityOnHand(quantity);
            item.setReorderLevel(reorder);
            inventoryItemRepository.save(item);
        } else {
            sku.setCostPrice(costPrice);
            sku.setSellingPrice(sellingPrice);
            skuRepository.save(sku);

            UUID skuId = sku.getId();
            InventoryItem item = inventoryItemRepository.findByOrganizationIdAndSkuId(orgId, skuId)
                    .orElseGet(() -> {
                        InventoryItem i = new InventoryItem();
                        i.setOrganizationId(orgId);
                        i.setSkuId(skuId);
                        return i;
                    });
            item.setQuantityOnHand(quantity);
            item.setReorderLevel(reorder);
            inventoryItemRepository.save(item);
        }
    }

    /** Finds the org's category by name (case-insensitive), creating it if the sheet references a new one. */
    private UUID resolveCategoryId(UUID orgId, String categoryName) {
        if (isBlank(categoryName)) {
            return null;
        }
        return categoryRepository.findByOrganizationIdAndNameIgnoreCase(orgId, categoryName.trim())
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setOrganizationId(orgId);
                    category.setName(categoryName.trim());
                    return categoryRepository.save(category);
                })
                .getId();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private BigDecimal parseMoney(String value, String field) {
        if (isBlank(value)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal bd = new BigDecimal(value.replace(",", "").trim());
            if (bd.signum() < 0) {
                throw new RowValidationException(field + " cannot be negative");
            }
            return bd;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Invalid number for " + field + ": " + value);
        }
    }

    private int parseInt(String value, String field, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            int v = Integer.parseInt(value.trim());
            if (v < 0) {
                throw new RowValidationException(field + " cannot be negative");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new RowValidationException("Invalid integer for " + field + ": " + value);
        }
    }
}
