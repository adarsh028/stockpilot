package com.stockpilot.inventory;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "channel_listings")
public class ChannelListing extends TenantEntity {

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "channel_sku")
    private String channelSku;

    @Column(name = "allocated_quantity", nullable = false)
    private int allocatedQuantity = 0;

    @Column(name = "channel_price")
    private BigDecimal channelPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChannelListingStatus status = ChannelListingStatus.ACTIVE;
}
