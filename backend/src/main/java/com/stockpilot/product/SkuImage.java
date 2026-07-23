package com.stockpilot.product;

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
@Table(name = "sku_images")
public class SkuImage extends TenantEntity {

    /** Marks a {@link #url} that holds a Google Drive file id ({@code gdrive:<fileId>}). */
    public static final String GDRIVE_PREFIX = "gdrive:";

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    /**
     * Opaque storage locator. Either {@code gdrive:<fileId>} for an image in the tenant's
     * Google Drive, or a legacy relative path ({@code /uploads/<uuid>.jpg}). Served through
     * a signed proxy URL (Drive) or absolute static URL (legacy) at response time.
     */
    @Column(nullable = false)
    private String url;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
}
