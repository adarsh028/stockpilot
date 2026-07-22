package com.stockpilot.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Product> findFirstByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    Page<Product> findByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganizationId(UUID organizationId);
}
