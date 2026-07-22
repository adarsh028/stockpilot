package com.stockpilot.product;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> ownedBy(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organizationId"), organizationId);
    }

    public static Specification<Product> search(String term) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String like = "%" + term.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("brandName")), like),
                cb.like(cb.lower(root.get("category")), like)
        );
    }

    public static Specification<Product> hasCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase());
    }

    public static Specification<Product> hasStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.valueOf(status.trim().toUpperCase()));
    }
}
