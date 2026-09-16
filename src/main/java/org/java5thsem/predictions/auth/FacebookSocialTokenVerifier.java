package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

final class FacebookSocialTokenVerifier {

    private final RestClient restClient;
    private final AuthProperties properties;

    FacebookSocialTokenVerifier(RestClient restClient, AuthProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    SocialProfile verify(String accessToken) {
        AuthProperties.FacebookConfig facebook = properties.socialOrEmpty().facebook();
        JsonNode debug = SocialProviderHttp.getJson(restClient, uriBuilder -> uriBuilder
                .host("graph.facebook.com")
                .path("/debug_token")
                .queryParam("input_token", accessToken)
                .queryParam("access_token", facebook.appId() + "|" + facebook.appSecret()));
        JsonNode data = debug.path("data");
        if (!SocialProviderHttp.flag(data, "is_valid")) {
            throw ApiException.invalidSocialToken();
        }
        String appId = SocialProviderHttp.text(data, "app_id");
        if (appId == null || !appId.equals(facebook.appId())) {
            throw ApiException.invalidSocialToken();
        }
        JsonNode me = SocialProviderHttp.getJson(restClient, uriBuilder -> uriBuilder
                .host("graph.facebook.com")
                .path("/me")
                .queryParam("fields", "id,name,email,picture.type(large)")
                .queryParam("access_token", accessToken));
        String subject = SocialProviderHttp.text(me, "id");
        if (subject == null) {
            throw ApiException.invalidSocialToken();
        }
        return new SocialProfile(
                SocialProvider.FACEBOOK,
                subject,
                SocialProviderHttp.text(me, "email"),
                SocialProviderHttp.text(me, "name"),
                SocialProviderHttp.text(me.path("picture").path("data"), "url")
        );
    }
}
