package com.stockpilot.sales;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class SaleSpecifications {

    private SaleSpecifications() {
    }

    public static Specification<Sale> ownedBy(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Sale> channel(UUID channelId) {
        if (channelId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("channelId"), channelId);
    }

    public static Specification<Sale> sku(UUID skuId) {
        if (skuId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("skuId"), skuId);
    }

    public static Specification<Sale> from(Instant from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("saleDate"), from);
    }

    public static Specification<Sale> to(Instant to) {
        if (to == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThan(root.get("saleDate"), to);
    }

    public static Specification<Sale> status(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), SaleStatus.valueOf(status.toUpperCase()));
    }
}
