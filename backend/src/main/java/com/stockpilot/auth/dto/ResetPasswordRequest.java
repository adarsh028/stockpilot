package com.stockpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {
}
