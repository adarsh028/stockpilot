package com.stockpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank String identifier
) {
}
