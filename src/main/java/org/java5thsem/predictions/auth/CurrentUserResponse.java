package org.java5thsem.predictions.auth;

import java.time.Instant;
import java.util.List;

public record CurrentUserResponse(
        Long userId,
        String username,
        String email,
        String displayName,
        String pictureUrl,
        Instant createdAt,
        List<SocialProvider> linkedProviders
) {
}
