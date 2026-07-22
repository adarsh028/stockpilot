package com.stockpilot.support;

import com.stockpilot.auth.VerificationPurpose;
import com.stockpilot.auth.VerificationToken;
import com.stockpilot.auth.VerificationTokenRepository;
import com.stockpilot.user.User;
import com.stockpilot.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Comparator;

/**
 * Base for integration tests. Spins up a real PostgreSQL via Testcontainers (never H2),
 * runs Flyway against it, and boots the full app on a random port.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    /**
     * By default we use Testcontainers (a real, ephemeral PostgreSQL — never H2), which is
     * the right choice in CI. On machines where the local Docker engine is too new for the
     * bundled docker-java client to negotiate, pass -Duse.local.postgres=true (and optionally
     * -Dlocal.postgres.url=...) to run against a dedicated local test database instead — the
     * spec explicitly allows "Testcontainers OR a dedicated local test Postgres".
     */
    static final boolean USE_LOCAL =
            Boolean.parseBoolean(System.getProperty("use.local.postgres", "false"));

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (USE_LOCAL) {
            POSTGRES = null;
        } else {
            POSTGRES = new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("stockpilot_test")
                    .withUsername("test")
                    .withPassword("test");
            POSTGRES.start();
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (USE_LOCAL) {
            registry.add("spring.datasource.url", () -> System.getProperty(
                    "local.postgres.url", "jdbc:postgresql://localhost:5432/stockpilot_test"));
            registry.add("spring.datasource.username",
                    () -> System.getProperty("local.postgres.username", "stockpilot"));
            registry.add("spring.datasource.password",
                    () -> System.getProperty("local.postgres.password", "stockpilot"));
            registry.add("spring.flyway.clean-disabled", () -> "false");
        } else {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        }
    }

    @Autowired
    protected TestRestTemplate rest;

    @org.junit.jupiter.api.BeforeEach
    void configureClient() {
        // Use Apache HttpClient5 (on the test classpath via Testcontainers) instead of the JDK
        // HttpURLConnection, which throws "cannot retry ... in streaming mode" on a 401 with a body.
        rest.getRestTemplate().setRequestFactory(
                new org.springframework.http.client.HttpComponentsClientHttpRequestFactory());
    }

    /** Fetch the first inventory SKU id for the caller's org (fresh test orgs have only their own). */
    protected String firstSkuId(String token) {
        org.springframework.http.ResponseEntity<String> inv = rest.exchange(
                "/api/v1/inventory?size=200", org.springframework.http.HttpMethod.GET,
                authGet(token), String.class);
        return extract(inv.getBody(), "skuId");
    }

    protected String firstChannelId(String token) {
        org.springframework.http.ResponseEntity<String> channels = rest.exchange(
                "/api/v1/channels", org.springframework.http.HttpMethod.GET,
                authGet(token), String.class);
        return extract(channels.getBody(), "id");
    }

    @Autowired
    protected VerificationTokenRepository verificationTokenRepository;

    @Autowired
    protected UserRepository userRepository;

    /** Sign up, verify via the OTP persisted in the DB, and return an access token + org id. */
    protected AuthContext signupAndLogin(String orgName, String email, String phone) {
        var signup = """
                {"organizationName":"%s","fullName":"Owner %s","email":"%s","phone":"%s","password":"Password123"}
                """.formatted(orgName, orgName, email, phone);
        rest.postForEntity("/api/v1/auth/signup", json(signup), String.class);

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String code = verificationTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(user.getId()))
                .filter(t -> t.getPurpose() == VerificationPurpose.SIGNUP_VERIFICATION)
                .filter(t -> t.getConsumedAt() == null && t.getExpiresAt().isAfter(Instant.now()))
                .max(Comparator.comparing(VerificationToken::getCreatedAt))
                .orElseThrow()
                .getCode();

        var verify = """
                {"identifier":"%s","code":"%s"}
                """.formatted(email, code);
        var response = rest.postForEntity("/api/v1/auth/verify-otp", json(verify), String.class);
        String body = response.getBody();
        String token = extract(body, "accessToken");
        String orgId = extract(body, "organizationId");
        return new AuthContext(token, orgId, user.getId().toString());
    }

    protected HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    protected HttpEntity<String> authed(String token, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return new HttpEntity<>(body, headers);
    }

    protected HttpEntity<Void> authGet(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return new HttpEntity<>(headers);
    }

    protected static String extract(String json, String field) {
        if (json == null) {
            return null;
        }
        String marker = "\"" + field + "\":\"";
        int idx = json.indexOf(marker);
        if (idx < 0) {
            return null;
        }
        int start = idx + marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    protected record AuthContext(String accessToken, String organizationId, String userId) {
    }
}
