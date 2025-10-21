package com.hhAutoApply.latr.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicReference;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    private final AppProperties appProperties;

    private final AtomicReference<String> accessToken = new AtomicReference<>("");


    @Bean
    public WebClient defaultWebClient() {
        return WebClient.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                        .maxInMemorySize(10 * 1024 * 1024))
                                .build();
    }

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder()
                .baseUrl(appProperties.getOauth_url())
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }


    @Bean
    public WebClient serviceWebClient() {
        return WebClient.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024))
                .build();
    }



}
