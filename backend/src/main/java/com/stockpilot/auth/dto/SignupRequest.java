package com.stockpilot.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank @Size(max = 150) String organizationName,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Pattern(regexp = "^[0-9+\\-\\s]{7,20}$", message = "invalid phone number") String phone,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}
