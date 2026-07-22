package com.stockpilot.analytics.dto;

import java.math.BigDecimal;

public record SalesByChannelResponse(
        String channelId,
        String channelName,
        long units,
        BigDecimal revenue
) {
}
