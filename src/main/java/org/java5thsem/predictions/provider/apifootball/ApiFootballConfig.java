package org.java5thsem.predictions.provider.apifootball;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ApiFootballProperties.class)
public class ApiFootballConfig {

    @Bean
    RestClient apiFootballRestClient(RestClient.Builder builder, ApiFootballProperties properties) {
        String apiKey = properties.apiKey() == null ? "" : properties.apiKey();
        return builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("x-apisports-key", apiKey)
                .build();
    }
}
