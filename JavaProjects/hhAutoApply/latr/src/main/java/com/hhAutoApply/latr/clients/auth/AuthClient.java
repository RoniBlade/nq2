package com.hhAutoApply.latr.clients.auth;

import com.hhAutoApply.latr.config.WebClientConfig;
import com.hhAutoApply.latr.exceptions.AuthException;
import com.hhAutoApply.latr.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final WebClient authWebClient;
    private final WebClientConfig webClientConfig;

    private final AppProperties appProperties;

    public String fetchAccessToken(String authCode) {

        return authWebClient.post()
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", appProperties.getClient_id())
                        .with("client_secret", appProperties.getClient_secret())
                        .with("code", authCode)
                        .with("redirect_uri", appProperties.getRedirect_uri())
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new AuthException("Ошибка клиента: " + response.statusCode()))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new AuthException("Ошибка сервера: " + response.statusCode()))
                )
                .bodyToMono(String.class)
                .retry(3)
                .map(body -> {
                    JSONObject jsonObject = new JSONObject(body);
                    if (!jsonObject.has("access_token")) {
                        throw new AuthException("Токен доступа отсутствует в ответе");
                    }

                    String accessToken = jsonObject.getString("access_token");

                    return accessToken;
                })
                .onErrorResume(e -> {
                    log.error("Ошибка при получении токена", e);
                    return Mono.error(new AuthException("Ошибка при получении токена: " + e.getMessage()));
                })
                .block();
    }

}