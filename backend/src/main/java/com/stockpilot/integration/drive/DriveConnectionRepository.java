package com.stockpilot.integration.drive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriveConnectionRepository extends JpaRepository<DriveConnection, UUID> {

    Optional<DriveConnection> findByOrganizationId(UUID organizationId);

    boolean existsByOrganizationId(UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}
