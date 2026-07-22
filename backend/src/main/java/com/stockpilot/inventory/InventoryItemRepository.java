package com.stockpilot.inventory;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByOrganizationIdAndSkuId(UUID organizationId, UUID skuId);

    Optional<InventoryItem> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<InventoryItem> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<InventoryItem> findByOrganizationId(UUID organizationId);

    @Query("select i from InventoryItem i where i.organizationId = :orgId and i.quantityOnHand <= i.reorderLevel")
    List<InventoryItem> findLowStock(@Param("orgId") UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.organizationId = :orgId and i.skuId = :skuId")
    Optional<InventoryItem> findForUpdate(@Param("orgId") UUID organizationId, @Param("skuId") UUID skuId);
}
