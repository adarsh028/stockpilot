package com.stockpilot.inventory;

import com.stockpilot.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "inventory_items")
public class InventoryItem extends TenantEntity {

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Column(name = "reorder_level", nullable = false)
    private int reorderLevel = 0;
}
