package com.stockpilot.tenant;

import com.stockpilot.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIsolationIT extends AbstractIntegrationTest {

    @Test
    void one_org_cannot_read_another_orgs_product() {
        AuthContext orgA = signupAndLogin("Org A", "a-owner@tenant.test", "+91 90000 30001");
        AuthContext orgB = signupAndLogin("Org B", "b-owner@tenant.test", "+91 90000 30002");

        // Org A creates a product
        String product = """
                {"name":"Secret Widget","skus":[{"sku":"A-SECRET-1","costPrice":10,"sellingPrice":20,"quantityOnHand":5,"reorderLevel":1}]}
                """;
        ResponseEntity<String> created = rest.postForEntity("/api/v1/products",
                authed(orgA.accessToken(), product), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String productId = extract(created.getBody(), "id");
        assertThat(productId).isNotBlank();

        // Org A can read it
        ResponseEntity<String> ownRead = rest.exchange("/api/v1/products/" + productId,
                HttpMethod.GET, authGet(orgA.accessToken()), String.class);
        assertThat(ownRead.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Org B must NOT — expect 404 (not 403, to avoid confirming existence)
        ResponseEntity<String> crossRead = rest.exchange("/api/v1/products/" + productId,
                HttpMethod.GET, authGet(orgB.accessToken()), String.class);
        assertThat(crossRead.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Org B must NOT be able to delete it either
        ResponseEntity<String> crossDelete = rest.exchange("/api/v1/products/" + productId,
                HttpMethod.DELETE, authGet(orgB.accessToken()), String.class);
        assertThat(crossDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
