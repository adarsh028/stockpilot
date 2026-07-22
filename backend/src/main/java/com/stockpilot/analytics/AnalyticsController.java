package com.stockpilot.analytics;

import com.stockpilot.analytics.dto.ChannelComparisonResponse;
import com.stockpilot.analytics.dto.SalesByChannelResponse;
import com.stockpilot.analytics.dto.SalesTrendPoint;
import com.stockpilot.analytics.dto.SummaryResponse;
import com.stockpilot.analytics.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public SummaryResponse summary(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return analyticsService.summary(preset, from, to);
    }

    @GetMapping("/sales-by-channel")
    public List<SalesByChannelResponse> salesByChannel(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return analyticsService.salesByChannel(preset, from, to);
    }

    @GetMapping("/sales-trend")
    public List<SalesTrendPoint> salesTrend(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "day") String granularity) {
        return analyticsService.salesTrend(preset, from, to, granularity);
    }

    @GetMapping("/top-products")
    public List<TopProductResponse> topProducts(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "10") int limit) {
        return analyticsService.topProducts(preset, from, to, limit);
    }

    @GetMapping("/channel-comparison")
    public List<ChannelComparisonResponse> channelComparison(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return analyticsService.channelComparison(preset, from, to);
    }
}
