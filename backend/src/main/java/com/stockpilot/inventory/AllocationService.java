package com.stockpilot.inventory;

import com.stockpilot.channel.Channel;
import com.stockpilot.channel.ChannelRepository;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.inventory.dto.ChannelListingRequest;
import com.stockpilot.inventory.dto.ChannelListingResponse;
import com.stockpilot.inventory.dto.ChannelListingUpdateRequest;
import com.stockpilot.product.Product;
import com.stockpilot.product.ProductRepository;
import com.stockpilot.product.Sku;
import com.stockpilot.product.SkuRepository;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AllocationService {

    private final ChannelListingRepository listingRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ChannelRepository channelRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final StockMovementService stockMovementService;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;

    @Transactional(readOnly = true)
    public List<ChannelListingResponse> listForChannel(UUID channelId) {
        UUID orgId = currentTenant.organizationId();
        tenantGuard.loadOwned("Channel", channelId, orgId,
                () -> channelRepository.findByIdAndOrganizationId(channelId, orgId));
        return listingRepository.findByOrganizationIdAndChannelId(orgId, channelId)
                .stream().map(l -> toResponse(orgId, l)).toList();
    }

    @Transactional
    public ChannelListingResponse upsert(UUID channelId, ChannelListingRequest req) {
        UUID orgId = currentTenant.organizationId();
        Channel channel = tenantGuard.loadOwned("Channel", channelId, orgId,
                () -> channelRepository.findByIdAndOrganizationId(channelId, orgId));
        Sku sku = skuRepository.findByIdAndOrganizationId(req.skuId(), orgId)
                .orElseThrow(() -> new ValidationException("Unknown SKU"));

        // Lock the inventory row for the duration to make the allocation-sum check race-safe.
        InventoryItem inventory = inventoryItemRepository.findForUpdate(orgId, sku.getId())
                .orElseThrow(() -> new ValidationException("No inventory record for this SKU"));

        int otherAllocations = listingRepository.sumAllocatedExcludingChannel(orgId, sku.getId(), channel.getId());
        if (otherAllocations + req.allocatedQuantity() > inventory.getQuantityOnHand()) {
            int available = inventory.getQuantityOnHand() - otherAllocations;
            throw new ValidationException("Allocation exceeds available stock. Available to allocate: "
                    + Math.max(0, available));
        }

        ChannelListing listing = listingRepository
                .findByOrganizationIdAndChannelIdAndSkuId(orgId, channel.getId(), sku.getId())
                .orElseGet(() -> {
                    ChannelListing l = new ChannelListing();
                    l.setOrganizationId(orgId);
                    l.setChannelId(channel.getId());
                    l.setSkuId(sku.getId());
                    return l;
                });

        int previous = listing.getAllocatedQuantity();
        listing.setChannelSku(req.channelSku());
        listing.setAllocatedQuantity(req.allocatedQuantity());
        listing.setChannelPrice(req.channelPrice());
        if (req.status() != null && !req.status().isBlank()) {
            listing.setStatus(ChannelListingStatus.valueOf(req.status().toUpperCase()));
        }
        listing = listingRepository.save(listing);

        stockMovementService.record(orgId, sku.getId(), channel.getId(), StockMovementType.ALLOCATION,
                req.allocatedQuantity() - previous, "Allocation set to " + req.allocatedQuantity(),
                listing.getId(), currentTenant.userId());

        return toResponse(orgId, listing);
    }

    @Transactional
    public ChannelListingResponse update(UUID listingId, ChannelListingUpdateRequest req) {
        UUID orgId = currentTenant.organizationId();
        ChannelListing listing = tenantGuard.loadOwned("Listing", listingId, orgId,
                () -> listingRepository.findByIdAndOrganizationId(listingId, orgId));

        if (req.allocatedQuantity() != null) {
            InventoryItem inventory = inventoryItemRepository.findForUpdate(orgId, listing.getSkuId())
                    .orElseThrow(() -> new ValidationException("No inventory record for this SKU"));
            int otherAllocations = listingRepository.sumAllocatedExcludingChannel(
                    orgId, listing.getSkuId(), listing.getChannelId());
            if (otherAllocations + req.allocatedQuantity() > inventory.getQuantityOnHand()) {
                int available = inventory.getQuantityOnHand() - otherAllocations;
                throw new ValidationException("Allocation exceeds available stock. Available to allocate: "
                        + Math.max(0, available));
            }
            int previous = listing.getAllocatedQuantity();
            listing.setAllocatedQuantity(req.allocatedQuantity());
            stockMovementService.record(orgId, listing.getSkuId(), listing.getChannelId(),
                    StockMovementType.ALLOCATION, req.allocatedQuantity() - previous,
                    "Allocation updated", listing.getId(), currentTenant.userId());
        }
        if (req.channelSku() != null) {
            listing.setChannelSku(req.channelSku());
        }
        if (req.channelPrice() != null) {
            listing.setChannelPrice(req.channelPrice());
        }
        if (req.status() != null && !req.status().isBlank()) {
            listing.setStatus(ChannelListingStatus.valueOf(req.status().toUpperCase()));
        }
        return toResponse(orgId, listingRepository.save(listing));
    }

    private ChannelListingResponse toResponse(UUID orgId, ChannelListing listing) {
        Channel channel = channelRepository.findByIdAndOrganizationId(listing.getChannelId(), orgId).orElse(null);
        Sku sku = skuRepository.findByIdAndOrganizationId(listing.getSkuId(), orgId).orElse(null);
        String productName = "";
        if (sku != null) {
            Product product = productRepository.findByIdAndOrganizationId(sku.getProductId(), orgId).orElse(null);
            if (product != null) {
                productName = product.getName();
            }
        }
        return new ChannelListingResponse(
                listing.getId().toString(),
                listing.getChannelId().toString(),
                channel != null ? channel.getName() : "",
                listing.getSkuId().toString(),
                sku != null ? sku.getSku() : "",
                productName,
                listing.getChannelSku(),
                listing.getAllocatedQuantity(),
                listing.getChannelPrice(),
                listing.getStatus().name()
        );
    }
}
