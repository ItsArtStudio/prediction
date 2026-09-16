package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;

final class CompositeSocialTokenVerifier implements SocialTokenVerifier {

    private final AuthProperties properties;
    private final GoogleSocialTokenVerifier google;
    private final AppleSocialTokenVerifier apple;
    private final FacebookSocialTokenVerifier facebook;

    CompositeSocialTokenVerifier(
            AuthProperties properties,
            GoogleSocialTokenVerifier google,
            AppleSocialTokenVerifier apple,
            FacebookSocialTokenVerifier facebook
    ) {
        this.properties = properties;
        this.google = google;
        this.apple = apple;
        this.facebook = facebook;
    }

    @Override
    public SocialProfile verify(SocialProvider provider, String idToken) {
        if (provider == null) {
            throw ApiException.authProviderUnsupported("unknown");
        }
        if (!properties.isConfigured(provider)) {
            throw ApiException.authProviderNotConfigured(provider.name());
        }
        return switch (provider) {
            case GOOGLE -> google.verify(idToken);
            case APPLE -> apple.verify(idToken);
            case FACEBOOK -> facebook.verify(idToken);
        };
    }
}
