package com.stockpilot.auth;

import com.stockpilot.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowIT extends AbstractIntegrationTest {

    @Test
    void signup_verify_login_refresh_flow() {
        AuthContext ctx = signupAndLogin("Acme Auth", "auth-owner@acme.test", "+91 90000 20001");
        assertThat(ctx.accessToken()).isNotBlank();

        // /me works with the token
        ResponseEntity<String> me = rest.exchange("/api/v1/auth/me",
                org.springframework.http.HttpMethod.GET, authGet(ctx.accessToken()), String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).contains("auth-owner@acme.test").contains("OWNER");

        // login returns tokens
        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                json("{\"identifier\":\"auth-owner@acme.test\",\"password\":\"Password123\"}"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String refreshToken = extract(login.getBody(), "refreshToken");
        assertThat(refreshToken).isNotBlank();

        // refresh rotates to a new access token
        ResponseEntity<String> refreshed = rest.postForEntity("/api/v1/auth/refresh",
                json("{\"refreshToken\":\"" + refreshToken + "\"}"), String.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(extract(refreshed.getBody(), "accessToken")).isNotBlank();
    }

    @Test
    void login_with_wrong_password_is_unauthorized() {
        signupAndLogin("Acme Auth2", "auth2@acme.test", "+91 90000 20002");
        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                json("{\"identifier\":\"auth2@acme.test\",\"password\":\"WrongPassword\"}"), String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
