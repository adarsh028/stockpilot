package com.stockpilot.product;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "skus")
public class Sku extends TenantEntity {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String sku;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributes = new HashMap<>();

    @Column(name = "cost_price", nullable = false)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false)
    private BigDecimal sellingPrice = BigDecimal.ZERO;
}
