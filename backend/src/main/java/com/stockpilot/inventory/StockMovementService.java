package com.stockpilot.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository repository;

    public void record(UUID orgId, UUID skuId, UUID channelId, StockMovementType type,
                       int quantityDelta, String reason, UUID referenceId, UUID createdBy) {
        StockMovement movement = new StockMovement();
        movement.setOrganizationId(orgId);
        movement.setSkuId(skuId);
        movement.setChannelId(channelId);
        movement.setType(type);
        movement.setQuantityDelta(quantityDelta);
        movement.setReason(reason);
        movement.setReferenceId(referenceId);
        movement.setCreatedBy(createdBy);
        repository.save(movement);
    }
}
