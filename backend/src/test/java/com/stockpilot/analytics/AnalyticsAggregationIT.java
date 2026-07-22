package com.stockpilot.analytics;

import com.stockpilot.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsAggregationIT extends AbstractIntegrationTest {

    @Test
    void summary_aggregates_recorded_sales() {
        AuthContext ctx = signupAndLogin("Analytics Org", "an-owner@an.test", "+91 90000 50001");
        String token = ctx.accessToken();

        String product = """
                {"name":"Analytics Product","skus":[{"sku":"AN-1","costPrice":50,"sellingPrice":100,"quantityOnHand":100,"reorderLevel":5}]}
                """;
        rest.postForEntity("/api/v1/products", authed(token, product), String.class);
        String skuId = firstSkuId(token);
        String channelId = firstChannelId(token);

        // Two sales today: 2 x 100 and 3 x 100 => units=5, revenue=500, orders=2
        recordSale(token, channelId, skuId, 2, "100.00");
        recordSale(token, channelId, skuId, 3, "100.00");

        ResponseEntity<String> summary = rest.exchange("/api/v1/analytics/summary?preset=LAST_7D",
                HttpMethod.GET, authGet(token), String.class);
        assertThat(summary.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = summary.getBody();
        assertThat(body).contains("\"orderCount\":2");
        assertThat(body).contains("\"unitsSold\":5");
        assertThat(body).contains("\"revenue\":500.00");

        // Sales-by-channel: the channel used should show revenue 500
        ResponseEntity<String> byChannel = rest.exchange("/api/v1/analytics/sales-by-channel?preset=LAST_7D",
                HttpMethod.GET, authGet(token), String.class);
        assertThat(byChannel.getBody()).contains("\"revenue\":500.00");

        // Top products should include our SKU with units 5
        ResponseEntity<String> top = rest.exchange("/api/v1/analytics/top-products?preset=LAST_7D&limit=5",
                HttpMethod.GET, authGet(token), String.class);
        assertThat(top.getBody()).contains("AN-1").contains("\"units\":5");
    }

    private void recordSale(String token, String channelId, String skuId, int qty, String unitPrice) {
        String sale = """
                {"channelId":"%s","skuId":"%s","quantity":%d,"unitPrice":%s}
                """.formatted(channelId, skuId, qty, unitPrice);
        rest.postForEntity("/api/v1/sales", authed(token, sale), String.class);
    }
}
