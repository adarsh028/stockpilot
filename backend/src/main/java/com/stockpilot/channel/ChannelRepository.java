package com.stockpilot.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelRepository extends JpaRepository<Channel, UUID> {

    List<Channel> findByOrganizationIdOrderByName(UUID organizationId);

    Optional<Channel> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Channel> findByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);

    Optional<Channel> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
}
