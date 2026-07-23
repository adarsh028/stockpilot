package com.stockpilot.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByOrganizationIdOrderByName(UUID organizationId);

    Optional<Category> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Category> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
