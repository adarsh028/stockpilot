package com.stockpilot.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChannelListingRepository extends JpaRepository<ChannelListing, UUID> {

    Optional<ChannelListing> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<ChannelListing> findByOrganizationIdAndChannelIdAndSkuId(UUID organizationId, UUID channelId, UUID skuId);

    List<ChannelListing> findByOrganizationIdAndChannelId(UUID organizationId, UUID channelId);

    List<ChannelListing> findByOrganizationIdAndSkuId(UUID organizationId, UUID skuId);

    @Query("""
            select coalesce(sum(l.allocatedQuantity), 0) from ChannelListing l
            where l.organizationId = :orgId and l.skuId = :skuId and l.channelId <> :excludeChannelId
            """)
    int sumAllocatedExcludingChannel(@Param("orgId") UUID orgId,
                                     @Param("skuId") UUID skuId,
                                     @Param("excludeChannelId") UUID excludeChannelId);
}
