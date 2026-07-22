package com.stockpilot.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UserInfo user
) {
    public record UserInfo(
            String id,
            String organizationId,
            String organizationName,
            String fullName,
            String email,
            String phone,
            String role
    ) {
    }
}
