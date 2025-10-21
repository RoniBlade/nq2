package org.example.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${midpoint.url}")
    private String midpointBaseUrl;

    @Value("${midpoint.username}")
    private String midpointUsername;

    @Value("${midpoint.password}")
    private String midpointPassword;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        return builder
                .baseUrl(midpointBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .filter(ExchangeFilterFunctions.basicAuthentication(midpointUsername, midpointPassword))
                .exchangeStrategies(strategies)
                .build();
    }
}
