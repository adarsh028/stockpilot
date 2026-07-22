package com.stockpilot.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Pattern(regexp = "OWNER|ADMIN|STAFF", message = "invalid role") String role,
        @Pattern(regexp = "ACTIVE|DISABLED", message = "invalid status") String status
) {
}
