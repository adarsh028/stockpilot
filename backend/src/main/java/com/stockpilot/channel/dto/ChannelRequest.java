package com.stockpilot.channel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChannelRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 50) String code,
        @NotBlank @Pattern(regexp = "MARKETPLACE|OWN_WEBSITE|OFFLINE|OTHER", message = "invalid channel type") String type,
        Boolean isActive
) {
}
