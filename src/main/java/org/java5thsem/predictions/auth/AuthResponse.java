package org.java5thsem.predictions.auth;

import java.time.Instant;
import java.util.List;

public record AuthResponse(
        Long userId,
        String username,
        String email,
        String displayName,
        String pictureUrl,
        SocialProvider provider,
        boolean newUser,
        String accessToken,
        String tokenType,
        Instant expiresAt,
        List<SocialProvider> linkedProviders
) {
}
