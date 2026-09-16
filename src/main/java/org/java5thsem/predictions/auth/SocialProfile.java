package org.java5thsem.predictions.auth;

public record SocialProfile(
        SocialProvider provider,
        String subject,
        String email,
        String displayName,
        String pictureUrl
) {
}
