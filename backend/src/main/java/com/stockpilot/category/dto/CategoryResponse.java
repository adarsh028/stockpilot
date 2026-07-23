package com.stockpilot.category.dto;

public record CategoryResponse(
        String id,
        String name,
        boolean isActive
) {
}
