package com.stockpilot.inventory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StockMovementRepository extends JpaRepository<StockMovement, UUID> {

    Page<StockMovement> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<StockMovement> findByOrganizationIdAndSkuIdOrderByCreatedAtDesc(UUID organizationId, UUID skuId, Pageable pageable);
}
