package com.stockpilot.user.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String organizationId,
        String fullName,
        String email,
        String phone,
        String role,
        String status,
        boolean emailVerified,
        Instant createdAt
) {
}
