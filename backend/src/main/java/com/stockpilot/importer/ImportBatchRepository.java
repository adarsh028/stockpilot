package com.stockpilot.importer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, UUID> {

    Page<ImportBatch> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<ImportBatch> findByOrganizationIdAndKindOrderByCreatedAtDesc(UUID organizationId, ImportKind kind, Pageable pageable);

    Optional<ImportBatch> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
