package com.stockpilot.sales;

import com.stockpilot.channel.Channel;
import com.stockpilot.channel.ChannelRepository;
import com.stockpilot.common.dto.PageResponse;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.inventory.ChannelListing;
import com.stockpilot.inventory.ChannelListingRepository;
import com.stockpilot.inventory.InventoryItem;
import com.stockpilot.inventory.InventoryItemRepository;
import com.stockpilot.inventory.LowStockNotifier;
import com.stockpilot.inventory.StockMovementService;
import com.stockpilot.inventory.StockMovementType;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.sales.dto.SaleRequest;
import com.stockpilot.sales.dto.SaleResponse;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ChannelRepository channelRepository;
    private final SkuRepository skuRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ChannelListingRepository channelListingRepository;
    private final StockMovementService stockMovementService;
    private final LowStockNotifier lowStockNotifier;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;
    private final SaleMapper saleMapper;

    @Value("${app.inventory.stock-policy:WARN}")
    private String stockPolicy;

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> list(UUID channelId, UUID skuId, Instant from, Instant to,
                                           String status, Pageable pageable) {
        UUID orgId = currentTenant.organizationId();
        Specification<Sale> spec = Specification.where(SaleSpecifications.ownedBy(orgId))
                .and(SaleSpecifications.channel(channelId))
                .and(SaleSpecifications.sku(skuId))
                .and(SaleSpecifications.from(from))
                .and(SaleSpecifications.to(to))
                .and(SaleSpecifications.status(status));
        Page<Sale> page = saleRepository.findAll(spec, pageable);
        return PageResponse.from(page, saleMapper::toResponse);
    }

    /** Manual sale entry from the API. */
    @Transactional
    public SaleResponse recordSale(SaleRequest req) {
        UUID orgId = currentTenant.organizationId();
        Channel channel = tenantGuard.loadOwned("Channel", req.channelId(), orgId,
                () -> channelRepository.findByIdAndOrganizationId(req.channelId(), orgId));
        Sku sku = tenantGuard.loadOwned("SKU", req.skuId(), orgId,
                () -> skuRepository.findByIdAndOrganizationId(req.skuId(), orgId));

        Instant saleDate = req.saleDate() != null ? req.saleDate() : Instant.now();
        Result result = record(orgId, channel.getId(), sku.getId(), req.quantity(), req.unitPrice(),
                saleDate, req.marketplaceOrderId(), SaleSource.MANUAL);
        return saleMapper.toResponse(result.sale(), result.warnings());
    }

    /**
     * Core sale recording used by both manual entry and CSV/XLSX import so the
     * inventory side effects live in exactly one place. Runs in the caller's transaction.
     */
    @Transactional
    public Result record(UUID orgId, UUID channelId, UUID skuId, int quantity, BigDecimal unitPrice,
                         Instant saleDate, String marketplaceOrderId, SaleSource source) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be greater than zero");
        }
        List<String> warnings = new ArrayList<>();

        InventoryItem inventory = inventoryItemRepository.findForUpdate(orgId, skuId)
                .orElseThrow(() -> new ValidationException("No inventory record for this SKU"));

        boolean negativeStock = inventory.getQuantityOnHand() - quantity < 0;
        if (negativeStock && "BLOCK".equalsIgnoreCase(stockPolicy)) {
            throw new ValidationException("Insufficient stock. On hand: " + inventory.getQuantityOnHand()
                    + ", requested: " + quantity);
        }
        if (negativeStock) {
            warnings.add("NEGATIVE_STOCK");
        }

        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Sale sale = new Sale();
        sale.setOrganizationId(orgId);
        sale.setChannelId(channelId);
        sale.setSkuId(skuId);
        sale.setQuantity(quantity);
        sale.setUnitPrice(unitPrice);
        sale.setTotalAmount(totalAmount);
        sale.setSaleDate(saleDate);
        sale.setMarketplaceOrderId(marketplaceOrderId);
        sale.setSource(source);
        sale.setStatus(SaleStatus.COMPLETED);
        sale = saleRepository.save(sale);

        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventoryItemRepository.save(inventory);

        channelListingRepository.findByOrganizationIdAndChannelIdAndSkuId(orgId, channelId, skuId)
                .ifPresent(listing -> {
                    listing.setAllocatedQuantity(Math.max(0, listing.getAllocatedQuantity() - quantity));
                    channelListingRepository.save(listing);
                });

        stockMovementService.record(orgId, skuId, channelId, StockMovementType.SALE,
                -quantity, "Sale " + sale.getId(), sale.getId(), currentTenant.userId());

        if (inventory.getQuantityOnHand() <= inventory.getReorderLevel()) {
            lowStockNotifier.notifyLowStock(orgId, skuId,
                    inventory.getQuantityOnHand(), inventory.getReorderLevel());
        }

        return new Result(sale, warnings);
    }

    @Transactional
    public SaleResponse returnSale(UUID saleId) {
        UUID orgId = currentTenant.organizationId();
        Sale sale = tenantGuard.loadOwned("Sale", saleId, orgId,
                () -> saleRepository.findByIdAndOrganizationId(saleId, orgId));
        if (sale.getStatus() == SaleStatus.RETURNED) {
            throw new ValidationException("Sale has already been returned");
        }

        sale.setStatus(SaleStatus.RETURNED);
        saleRepository.save(sale);

        InventoryItem inventory = inventoryItemRepository.findForUpdate(orgId, sale.getSkuId())
                .orElseThrow(() -> new ValidationException("No inventory record for this SKU"));
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() + sale.getQuantity());
        inventoryItemRepository.save(inventory);

        channelListingRepository.findByOrganizationIdAndChannelIdAndSkuId(orgId, sale.getChannelId(), sale.getSkuId())
                .ifPresent(listing -> {
                    listing.setAllocatedQuantity(listing.getAllocatedQuantity() + sale.getQuantity());
                    channelListingRepository.save(listing);
                });

        stockMovementService.record(orgId, sale.getSkuId(), sale.getChannelId(), StockMovementType.RETURN,
                sale.getQuantity(), "Return of sale " + sale.getId(), sale.getId(), currentTenant.userId());

        return saleMapper.toResponse(sale);
    }

    @Transactional
    public void delete(UUID saleId) {
        UUID orgId = currentTenant.organizationId();
        Sale sale = tenantGuard.loadOwned("Sale", saleId, orgId,
                () -> saleRepository.findByIdAndOrganizationId(saleId, orgId));
        // Reverse stock impact only if the sale was still counted (COMPLETED).
        if (sale.getStatus() == SaleStatus.COMPLETED) {
            inventoryItemRepository.findForUpdate(orgId, sale.getSkuId()).ifPresent(inv -> {
                inv.setQuantityOnHand(inv.getQuantityOnHand() + sale.getQuantity());
                inventoryItemRepository.save(inv);
            });
            stockMovementService.record(orgId, sale.getSkuId(), sale.getChannelId(), StockMovementType.ADJUSTMENT,
                    sale.getQuantity(), "Sale deleted " + sale.getId(), sale.getId(), currentTenant.userId());
        }
        saleRepository.delete(sale);
    }

    public record Result(Sale sale, List<String> warnings) {
    }
}
