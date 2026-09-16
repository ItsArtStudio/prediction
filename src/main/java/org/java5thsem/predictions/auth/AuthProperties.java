package org.java5thsem.predictions.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(
        Duration sessionTtl,
        Social social
) {
    public Duration sessionTtlOrDefault() {
        return sessionTtl == null ? Duration.ofDays(30) : sessionTtl;
    }

    public Social socialOrEmpty() {
        return social == null ? new Social(null, null, null) : social;
    }

    public boolean isConfigured(SocialProvider provider) {
        return switch (provider) {
            case GOOGLE -> socialOrEmpty().googleConfigured();
            case APPLE -> socialOrEmpty().appleConfigured();
            case FACEBOOK -> socialOrEmpty().facebookConfigured();
        };
    }

    public record Social(
            ProviderConfig google,
            ProviderConfig apple,
            FacebookConfig facebook
    ) {
        boolean googleConfigured() {
            return google != null && google.configured();
        }

        boolean appleConfigured() {
            return apple != null && apple.configured();
        }

        boolean facebookConfigured() {
            return facebook != null && facebook.configured();
        }
    }

    public record ProviderConfig(String clientId) {
        boolean configured() {
            return clientId != null && !clientId.isBlank();
        }
    }

    public record FacebookConfig(String appId, String appSecret) {
        boolean configured() {
            return appId != null && !appId.isBlank()
                    && appSecret != null && !appSecret.isBlank();
        }
    }
}
