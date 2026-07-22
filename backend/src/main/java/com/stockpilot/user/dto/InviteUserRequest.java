package com.stockpilot.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InviteUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "invalid phone number") String phone,
        @NotBlank @Pattern(regexp = "ADMIN|STAFF", message = "role must be ADMIN or STAFF") String role,
        @Size(min = 8, max = 100) String password
) {
}
