package com.stockpilot.product;

import com.stockpilot.common.exception.UnauthorizedException;
import com.stockpilot.security.SignedTokenService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Signs and verifies the short-lived tokens embedded in image-proxy URLs. Because
 * {@code <img>} tags can't send an Authorization header, the tenant is carried in a
 * signed token in the query string instead. The token binds the image id to the org so
 * one org's URL can never be used to read another's images.
 */
@Component
@RequiredArgsConstructor
public class ImageUrlSigner {

    static final String PURPOSE = "image_content";
    private static final Duration TTL = Duration.ofHours(6);

    private final SignedTokenService signedTokenService;

    /** Issues a token authorizing read access to {@code imageId} on behalf of {@code orgId}. */
    public String issue(UUID orgId, UUID imageId) {
        return signedTokenService.issue(PURPOSE,
                Map.of("orgId", orgId.toString(), "imageId", imageId.toString()), TTL);
    }

    /** Verifies the token and that it was issued for {@code imageId}; returns the org id. */
    public UUID verify(String token, UUID imageId) {
        Claims claims = signedTokenService.verify(token, PURPOSE);
        if (!imageId.toString().equals(claims.get("imageId", String.class))) {
            throw new UnauthorizedException("Token does not match the requested image");
        }
        return UUID.fromString(claims.get("orgId", String.class));
    }
}
