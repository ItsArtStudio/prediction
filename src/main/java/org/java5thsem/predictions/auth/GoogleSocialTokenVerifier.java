package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

final class GoogleSocialTokenVerifier {

    private final RestClient restClient;
    private final AuthProperties properties;

    GoogleSocialTokenVerifier(RestClient restClient, AuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    SocialProfile verify(String idToken) {
        JsonNode payload = SocialProviderHttp.getJson(restClient, uriBuilder -> uriBuilder
                .host("oauth2.googleapis.com")
                .path("/tokeninfo")
                .queryParam("id_token", idToken));
        String audience = SocialProviderHttp.text(payload, "aud");
        String authorizedParty = SocialProviderHttp.text(payload, "azp");
        AuthProperties.ProviderConfig google = properties.socialOrEmpty().google();
        String clientId = google == null ? null : google.clientId();
        if (clientId == null || (!clientId.equals(audience) && !clientId.equals(authorizedParty))) {
            throw ApiException.invalidSocialToken();
        }
        String subject = SocialProviderHttp.text(payload, "sub");
        if (subject == null) {
            throw ApiException.invalidSocialToken();
        }
        String email = SocialProviderHttp.flag(payload, "email_verified")
                ? SocialProviderHttp.text(payload, "email")
                : null;
        return new SocialProfile(
                SocialProvider.GOOGLE,
                subject,
                email,
                SocialProviderHttp.text(payload, "name"),
                SocialProviderHttp.text(payload, "picture")
        );
    }
}
