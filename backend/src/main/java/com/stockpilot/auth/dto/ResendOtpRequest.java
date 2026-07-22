package com.stockpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
        @NotBlank String identifier
) {
}
