package com.stockpilot.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank String identifier,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "code must be 6 digits") String code
) {
}
