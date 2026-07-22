package com.stockpilot.auth;

import com.stockpilot.common.exception.RateLimitExceededException;
import com.stockpilot.common.exception.ValidationException;
import com.stockpilot.email.OtpSender;
import com.stockpilot.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final VerificationTokenRepository tokenRepository;
    private final OtpRequestLogRepository requestLogRepository;
    private final OtpSender otpSender;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.otp.ttl-minutes:10}")
    private long ttlMinutes;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private long cooldownSeconds;

    @Value("${app.otp.max-per-hour:5}")
    private long maxPerHour;

    /**
     * Generate + send a fresh OTP for a user, enforcing per-user rate limits.
     * Invalidates any prior unconsumed code for the same purpose.
     */
    @Transactional
    public void issueOtp(User user, VerificationPurpose purpose) {
        Instant now = Instant.now();

        long recent = requestLogRepository.countByUserIdAndPurposeAndRequestedAtAfter(
                user.getId(), purpose, now.minusSeconds(cooldownSeconds));
        if (recent > 0) {
            throw new RateLimitExceededException("Please wait before requesting another code");
        }
        long lastHour = requestLogRepository.countByUserIdAndPurposeAndRequestedAtAfter(
                user.getId(), purpose, now.minus(1, ChronoUnit.HOURS));
        if (lastHour >= maxPerHour) {
            throw new RateLimitExceededException("Too many code requests. Try again later");
        }

        tokenRepository.deleteUnconsumed(user.getId(), purpose);

        String code = String.format("%06d", random.nextInt(1_000_000));
        VerificationToken token = new VerificationToken();
        token.setUserId(user.getId());
        token.setCode(code);
        token.setPurpose(purpose);
        token.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(token);

        OtpRequestLog logEntry = new OtpRequestLog();
        logEntry.setUserId(user.getId());
        logEntry.setPurpose(purpose);
        logEntry.setRequestedAt(now);
        requestLogRepository.save(logEntry);

        otpSender.sendOtp(user, code, purpose);
    }

    /**
     * Validate and consume an OTP. Throws if invalid/expired/already used.
     */
    @Transactional
    public void verifyAndConsume(User user, String code, VerificationPurpose purpose) {
        VerificationToken token = tokenRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                        user.getId(), purpose, Instant.now())
                .orElseThrow(() -> new ValidationException("Code is invalid or has expired"));

        if (!token.getCode().equals(code)) {
            throw new ValidationException("Code is invalid or has expired");
        }
        token.setConsumedAt(Instant.now());
        tokenRepository.save(token);
    }
}
