package com.stockpilot.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID> {

    List<Sku> findByProductIdAndOrganizationId(UUID productId, UUID organizationId);

    List<Sku> findByOrganizationId(UUID organizationId);

    Optional<Sku> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Sku> findByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);

    boolean existsByOrganizationIdAndSkuIgnoreCase(UUID organizationId, String sku);
}
