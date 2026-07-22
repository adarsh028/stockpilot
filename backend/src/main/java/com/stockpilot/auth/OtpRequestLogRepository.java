package com.stockpilot.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface OtpRequestLogRepository extends JpaRepository<OtpRequestLog, UUID> {

    long countByUserIdAndPurposeAndRequestedAtAfter(UUID userId, VerificationPurpose purpose, Instant after);
}
