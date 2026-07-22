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

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "stock_movements")
public class StockMovement extends TenantEntity {

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "channel_id")
    private UUID channelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockMovementType type;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    private String reason;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "created_by")
    private UUID createdBy;
}
