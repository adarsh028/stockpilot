package com.stockpilot.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpilot.common.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Per-IP rate limit on sensitive unauthenticated auth endpoints (OTP request/verify,
 * forgot/reset password) to blunt brute-force and email-spam abuse.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/resend-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/login"
    );
    private static final int MAX_REQUESTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final InMemoryRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (LIMITED_PATHS.contains(path)) {
            String key = clientIp(request) + ":" + path;
            if (!rateLimiter.tryAcquire(key, MAX_REQUESTS, WINDOW_SECONDS)) {
                writeTooMany(request, response);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void writeTooMany(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Too many requests. Please slow down and try again shortly.",
                request.getRequestURI(),
                List.of()
        );
        objectMapper.writeValue(response.getWriter(), error);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
