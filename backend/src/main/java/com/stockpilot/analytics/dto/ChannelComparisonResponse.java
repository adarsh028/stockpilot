package com.stockpilot.analytics.dto;

import java.math.BigDecimal;

public record ChannelComparisonResponse(
        String channelId,
        String channelName,
        long units,
        BigDecimal revenue,
        long unitsPrevious,
        BigDecimal revenuePrevious,
        BigDecimal revenueChangePct
) {
}
