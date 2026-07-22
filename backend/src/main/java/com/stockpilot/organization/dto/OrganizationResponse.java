package com.stockpilot.organization.dto;

import java.time.Instant;

public record OrganizationResponse(
        String id,
        String name,
        String slug,
        String plan,
        String status,
        Instant createdAt
) {
}
