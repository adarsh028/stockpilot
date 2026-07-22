package com.stockpilot.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Base class for every tenant-scoped entity. Carrying organization_id on the
 * entity itself lets the service/AOP layer verify ownership on every load.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends BaseEntity {

    @Column(name = "organization_id", nullable = false, updatable = false)
    private UUID organizationId;
}
