package com.stockpilot.product;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "products")
public class Product extends TenantEntity {

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "brand_name")
    private String brandName;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status = ProductStatus.ACTIVE;
}
