package com.stockpilot.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // System-wide uniqueness checks (deliberate cross-tenant lookups, used only at signup/invite).
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    // Tenant-scoped access.
    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Page<User> findByOrganizationId(UUID organizationId, Pageable pageable);
}
