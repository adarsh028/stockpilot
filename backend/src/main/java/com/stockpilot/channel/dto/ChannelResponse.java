package com.stockpilot.channel.dto;

public record ChannelResponse(
        String id,
        String name,
        String code,
        String type,
        boolean isActive
) {
}
