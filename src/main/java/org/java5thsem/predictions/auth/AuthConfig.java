package org.java5thsem.predictions.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {

    @Bean
    SocialTokenVerifier socialTokenVerifier(
            RestClient.Builder restClientBuilder,
            AuthProperties properties,
            Clock clock
    ) {
        RestClient restClient = restClientBuilder.clone().build();
        return new CompositeSocialTokenVerifier(
                properties,
                new GoogleSocialTokenVerifier(restClient, properties),
                new AppleSocialTokenVerifier(restClient, properties, clock),
                new FacebookSocialTokenVerifier(restClient, properties)
        );
    }
}
