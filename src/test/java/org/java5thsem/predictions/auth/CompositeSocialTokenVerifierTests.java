package org.java5thsem.predictions.auth;

import org.java5thsem.predictions.api.ApiException;
import org.java5thsem.predictions.api.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompositeSocialTokenVerifierTests {

    @Test
    void unconfiguredProviderIsServiceUnavailable() {
        AuthProperties properties = new AuthProperties(Duration.ofDays(30), new AuthProperties.Social(
                new AuthProperties.ProviderConfig(""),
                new AuthProperties.ProviderConfig(""),
                new AuthProperties.FacebookConfig("", "")
        ));
        CompositeSocialTokenVerifier verifier = new CompositeSocialTokenVerifier(properties, null, null, null);

        assertThatThrownBy(() -> verifier.verify(SocialProvider.GOOGLE, "token"))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.AUTH_PROVIDER_NOT_CONFIGURED);
    }

    @Test
    void listsEnabledProvidersFromConfiguration() {
        AuthProperties properties = new AuthProperties(Duration.ofDays(30), new AuthProperties.Social(
                new AuthProperties.ProviderConfig("google-client"),
                new AuthProperties.ProviderConfig(""),
                new AuthProperties.FacebookConfig("app", "secret")
        ));

        assertThat(properties.isConfigured(SocialProvider.GOOGLE)).isTrue();
        assertThat(properties.isConfigured(SocialProvider.APPLE)).isFalse();
        assertThat(properties.isConfigured(SocialProvider.FACEBOOK)).isTrue();
    }
}
