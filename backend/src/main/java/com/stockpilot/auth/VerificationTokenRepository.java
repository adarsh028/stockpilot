package com.stockpilot.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    Optional<VerificationToken> findFirstByUserIdAndPurposeAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            UUID userId, VerificationPurpose purpose, Instant now);

    @Modifying
    @Query("delete from VerificationToken t where t.userId = :userId and t.purpose = :purpose and t.consumedAt is null")
    void deleteUnconsumed(@Param("userId") UUID userId, @Param("purpose") VerificationPurpose purpose);
}
