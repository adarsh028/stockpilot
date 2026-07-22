package com.stockpilot.sales;

import com.stockpilot.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SaleInventoryDecrementIT extends AbstractIntegrationTest {

    @Test
    void recording_a_sale_decrements_inventory_and_return_reverses_it() {
        AuthContext ctx = signupAndLogin("Sales Org", "sales-owner@sale.test", "+91 90000 40001");
        String token = ctx.accessToken();

        // Create product with one SKU starting at qty 10
        String product = """
                {"name":"Decrement Test","skus":[{"sku":"DEC-1","costPrice":5,"sellingPrice":15,"quantityOnHand":10,"reorderLevel":2}]}
                """;
        ResponseEntity<String> created = rest.postForEntity("/api/v1/products",
                authed(token, product), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String skuId = firstSkuId(token);
        assertThat(skuId).isNotBlank();

        // Pick a channel
        String channelId = firstChannelId(token);
        assertThat(channelId).isNotBlank();

        // Record a sale of qty 3
        String sale = """
                {"channelId":"%s","skuId":"%s","quantity":3,"unitPrice":15.00}
                """.formatted(channelId, skuId);
        ResponseEntity<String> saleResp = rest.postForEntity("/api/v1/sales", authed(token, sale), String.class);
        assertThat(saleResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String saleId = extract(saleResp.getBody(), "id");

        // Inventory should now be 7
        assertThat(quantityFor(token, skuId)).isEqualTo(7);

        // Return the sale — inventory should go back to 10
        rest.postForEntity("/api/v1/sales/" + saleId + "/return", authed(token, ""), String.class);
        assertThat(quantityFor(token, skuId)).isEqualTo(10);
    }

    @Test
    void selling_more_than_stock_flags_a_negative_stock_warning_but_succeeds() {
        AuthContext ctx = signupAndLogin("Warn Org", "warn-owner@sale.test", "+91 90000 40002");
        String token = ctx.accessToken();

        String product = """
                {"name":"Warn Test","skus":[{"sku":"WARN-1","costPrice":5,"sellingPrice":15,"quantityOnHand":2,"reorderLevel":1}]}
                """;
        rest.postForEntity("/api/v1/products", authed(token, product), String.class);
        String skuId = firstSkuId(token);
        String channelId = firstChannelId(token);

        String sale = """
                {"channelId":"%s","skuId":"%s","quantity":5,"unitPrice":15.00}
                """.formatted(channelId, skuId);
        ResponseEntity<String> resp = rest.postForEntity("/api/v1/sales", authed(token, sale), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).contains("NEGATIVE_STOCK");
        assertThat(quantityFor(token, skuId)).isEqualTo(-3);
    }

    private int quantityFor(String token, String skuId) {
        ResponseEntity<String> inv = rest.exchange("/api/v1/inventory?size=200",
                HttpMethod.GET, authGet(token), String.class);
        String body = inv.getBody();
        int idx = body.indexOf("\"skuId\":\"" + skuId + "\"");
        assertThat(idx).isGreaterThan(-1);
        String marker = "\"quantityOnHand\":";
        int q = body.indexOf(marker, idx);
        int start = q + marker.length();
        int end = start;
        while (end < body.length() && (Character.isDigit(body.charAt(end)) || body.charAt(end) == '-')) {
            end++;
        }
        return Integer.parseInt(body.substring(start, end));
    }
}
