package com.stockpilot.sales;

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
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "sales")
public class Sale extends TenantEntity {

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @Column(name = "marketplace_order_id")
    private String marketplaceOrderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleSource source = SaleSource.MANUAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SaleStatus status = SaleStatus.COMPLETED;
}
