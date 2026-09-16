package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Consumer;

final class SocialProviderHttp {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private SocialProviderHttp() {
    }

    static JsonNode getJson(RestClient restClient, Consumer<UriBuilder> uri) {
        try {
            return JSON.readTree(getBody(restClient, uri));
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.providerUnavailable("Could not parse the social login provider response");
        }
    }

    static String getBody(RestClient restClient, Consumer<UriBuilder> uri) {
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.scheme("https");
                        uri.accept(uriBuilder);
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw ApiException.invalidSocialToken();
                    })
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw ApiException.providerUnavailable(
                                "Social login provider request failed with HTTP " + response.getStatusCode().value());
                    })
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw ApiException.providerUnavailable("Social login provider returned an empty response");
            }
            return body;
        } catch (ApiException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().is4xxClientError()) {
                throw ApiException.invalidSocialToken();
            }
            throw ApiException.providerUnavailable(
                    "Social login provider request failed with HTTP " + exception.getStatusCode().value());
        } catch (RestClientException exception) {
            throw ApiException.providerUnavailable("Could not reach the social login provider");
        }
    }

    static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        return textValue(node.path(field));
    }

    static String textValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asString();
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    static boolean flag(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asString();
        return "true".equalsIgnoreCase(text) || "1".equals(text);
    }
}
