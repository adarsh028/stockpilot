package com.stockpilot.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuImageRepository extends JpaRepository<SkuImage, UUID> {

    List<SkuImage> findBySkuIdAndOrganizationIdOrderByPrimaryDescSortOrderAscCreatedAtAsc(
            UUID skuId, UUID organizationId);

    List<SkuImage> findBySkuIdAndOrganizationId(UUID skuId, UUID organizationId);

    Optional<SkuImage> findByIdAndOrganizationId(UUID id, UUID organizationId);

    long countBySkuIdAndOrganizationId(UUID skuId, UUID organizationId);
}
