package com.stockpilot.sales;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID>, JpaSpecificationExecutor<Sale> {

    Optional<Sale> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<Sale> findByOrganizationId(UUID organizationId, Pageable pageable);
}
