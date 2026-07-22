package com.stockpilot.analytics;

import com.stockpilot.analytics.dto.ChannelComparisonResponse;
import com.stockpilot.analytics.dto.SalesByChannelResponse;
import com.stockpilot.analytics.dto.SalesTrendPoint;
import com.stockpilot.analytics.dto.SummaryResponse;
import com.stockpilot.analytics.dto.TopProductResponse;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.common.util.DateRange;
import com.stockpilot.common.util.DateRangeResolver;
import com.stockpilot.inventory.InventoryItemRepository;
import com.stockpilot.tenant.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final DateRangeResolver dateRangeResolver;
    private final CurrentTenant currentTenant;

    @Transactional(readOnly = true)
    public SummaryResponse summary(String preset, String from, String to) {
        UUID orgId = currentTenant.organizationId();
        DateRange range = dateRangeResolver.resolve(preset, from, to);
        DateRange previous = range.previousPeriod();

        AnalyticsRepository.Totals current = analyticsRepository.totals(orgId, range);
        AnalyticsRepository.Totals prior = analyticsRepository.totals(orgId, previous);
        long lowStockCount = inventoryItemRepository.findLowStock(orgId).size();

        BigDecimal changePct = percentChange(prior.revenue(), current.revenue());

        return new SummaryResponse(
                current.orderCount(),
                current.units(),
                current.revenue(),
                lowStockCount,
                changePct,
                prior.orderCount(),
                prior.revenue(),
                range.from().toString(),
                range.to().toString()
        );
    }

    @Transactional(readOnly = true)
    public List<SalesByChannelResponse> salesByChannel(String preset, String from, String to) {
        UUID orgId = currentTenant.organizationId();
        DateRange range = dateRangeResolver.resolve(preset, from, to);
        return analyticsRepository.salesByChannel(orgId, range);
    }

    @Transactional(readOnly = true)
    public List<SalesTrendPoint> salesTrend(String preset, String from, String to, String granularity) {
        UUID orgId = currentTenant.organizationId();
        DateRange range = dateRangeResolver.resolve(preset, from, to);
        return analyticsRepository.salesTrend(orgId, range, granularityLiteral(granularity));
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(String preset, String from, String to, int limit) {
        UUID orgId = currentTenant.organizationId();
        DateRange range = dateRangeResolver.resolve(preset, from, to);
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return analyticsRepository.topProducts(orgId, range, safeLimit);
    }

    @Transactional(readOnly = true)
    public List<ChannelComparisonResponse> channelComparison(String preset, String from, String to) {
        UUID orgId = currentTenant.organizationId();
        DateRange range = dateRangeResolver.resolve(preset, from, to);
        DateRange previous = range.previousPeriod();

        List<SalesByChannelResponse> current = analyticsRepository.salesByChannel(orgId, range);
        Map<String, SalesByChannelResponse> priorByChannel = new HashMap<>();
        for (SalesByChannelResponse r : analyticsRepository.salesByChannel(orgId, previous)) {
            priorByChannel.put(r.channelId(), r);
        }

        return current.stream().map(c -> {
            SalesByChannelResponse prior = priorByChannel.get(c.channelId());
            long unitsPrev = prior != null ? prior.units() : 0;
            BigDecimal revPrev = prior != null ? prior.revenue() : BigDecimal.ZERO;
            return new ChannelComparisonResponse(
                    c.channelId(), c.channelName(), c.units(), c.revenue(),
                    unitsPrev, revPrev, percentChange(revPrev, c.revenue()));
        }).toList();
    }

    private String granularityLiteral(String granularity) {
        String g = granularity == null ? "day" : granularity.trim().toLowerCase();
        return switch (g) {
            case "day" -> "day";
            case "week" -> "week";
            case "month" -> "month";
            default -> throw new ValidationException("granularity must be day, week, or month");
        };
    }

    private BigDecimal percentChange(BigDecimal previous, BigDecimal current) {
        if (previous == null || previous.signum() == 0) {
            return current != null && current.signum() > 0 ? new BigDecimal("100.00") : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }
}
