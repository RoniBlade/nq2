package org.example.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MidPointClient {

    private final WebClient webClient;
    private final RestTemplate restTemplate;

    @Value("${midpoint.url}")
    private String midpointBaseUrl;

    private WebClient.Builder getClient(String authHeader) {


        return WebClient.builder()
                .baseUrl(midpointBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader);
    }

    public Mono<String> createObject(String objectType, Map<String, Object> objectData, String authHeader) {
        Map<String, Object> body = Map.of(objectType, objectData);

        WebClient client = getClient(authHeader).defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        log.info("Отправка запроса на создание объекта [{}]: {}", objectType, body);

        return client.post()
                .uri("/ws/rest/" + objectType + "s")
                .bodyValue(body)
                .exchangeToMono(response -> response.toEntity(String.class).flatMap(entity -> {
                    if (entity.getStatusCode().is2xxSuccessful()) {
                        String location = entity.getHeaders().getFirst("Location");
                        log.info("📍 Location: {}", location);
                        if (location != null && location.contains("/")) {
                            String oid = location.substring(location.lastIndexOf('/') + 1);
                            return Mono.just(oid);
                        } else {
                            return Mono.just("NO_OID_IN_LOCATION");
                        }
                    } else {
                        log.error("❌ Ошибка от MidPoint ({}): {}", entity.getStatusCode(), entity.getBody());
                        return Mono.error(new ResponseStatusException(entity.getStatusCode(), entity.getBody()));
                    }
                }));
    }

    public Mono<String> updateObject(String objectType, UUID oid, Map<String, Object> body, String authHeader) {
        log.info("Отправка запроса на обновление объекта [{}], oid={} с телом: {}", objectType, oid, body);

        WebClient client = getClient(authHeader).defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();


        log.info(body.toString());
        return client.patch()
                .uri("/ws/rest/" + objectType + "s/{oid}", oid)
                .bodyValue(body)
                .exchangeToMono(this::handleResponse);
    }

    public Mono<Void> deleteObject(String objectType, UUID oid, String authHeader) {
        log.info("🗑️ Отправка запроса на удаление объекта [{}] с OID={}", objectType, oid);

        WebClient client = getClient(authHeader).defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();

        return client.delete()
                .uri("/ws/rest/" + objectType + "s/{oid}", oid)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        log.info("✅ Объект [{}] с OID={} успешно удалён", objectType, oid);
                        return Mono.empty();
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    log.error("❌ Ошибка при удалении объекта [{}], OID={}, статус={}, тело={}",
                                            objectType, oid, response.statusCode(), body);
                                    return Mono.error(new ResponseStatusException(response.statusCode(), body));
                                });
                    }
                });
    }

    private Mono<String> handleResponse(ClientResponse response) {
        int status = response.statusCode().value();

        return response.bodyToMono(String.class).flatMap(body -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Успешный ответ от MidPoint: {}", body);
                return Mono.just(body);
            } else if (status == 409) {
                String oid = null;
                try {
                    int i = body.indexOf("user:");
                    int j = body.indexOf("(", i);
                    if (i != -1 && j != -1) {
                        oid = body.substring(i + 5, j);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Не удалось извлечь OID: {}", e.getMessage());
                }
                log.warn("⚠️ Конфликт: объект уже существует, OID={}", oid);
                return Mono.just("EXISTS:" + (oid != null ? oid : "UNKNOWN"));
            } else {
                log.error("❌ Ошибка от MidPoint ({}): {}", status, body);
                return Mono.error(new ResponseStatusException(response.statusCode(), body));
            }
        });
    }


    public Mono<ResponseEntity<?>> openNextStage(UUID oid, String authHeader) {

        log.info("authHeaders setuped");
        WebClient client = getClient(authHeader).build();
        return webClient.post().uri("/api/glt/certificationService/" + oid + "/openNextStage").exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Переход на следующий этап ресертификации выполнен", oid);
                return Mono.empty();
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ Ошибка при переходе на следующий этап ресертификации, OID={}, статус={}, тело={}",
                                    oid, response.statusCode(), body);
                            return Mono.error(new ResponseStatusException(response.statusCode(), body));
                        });
            }
        });
    }

    public Mono<ResponseEntity<?>> closeCurrentStage(UUID oid, String authHeader) {

        WebClient client = getClient(authHeader).build();

        return client.post().uri("/api/glt/certificationService/" + oid + "/closeCurrentStage").exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Завершение текущего этапа ресертификации выполнено", oid);
                return Mono.empty();
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ Ошибка при завершении текущего этапа ресертификации, OID={}, статус={}, тело={}",
                                    oid, response.statusCode(), body);
                            return Mono.error(new ResponseStatusException(response.statusCode(), body));
                        });
            }
        });
    }

    public Mono<ResponseEntity<?>> closeCampaign(UUID oid, String authHeader) {

        WebClient client = getClient(authHeader).build();

        return client.post().uri("/api/glt/certificationService/" + oid + "/closeCampaign").exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Завершение ресертификации выполнено", oid);
                return Mono.empty();
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ Ошибка при завершении ресертификации, OID={}, статус={}, тело={}",
                                    oid, response.statusCode(), body);
                            return Mono.error(new ResponseStatusException(response.statusCode(), body));
                        });
            }
        });
    }

    public Mono<ResponseEntity<?>> reiterateCampaign(UUID oid, String authHeader) {

        WebClient client = getClient(authHeader).build();

        return client.post().uri("/api/glt/certificationService/" + oid + "/reiterateCampaign").exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Перезапуск ресертификации выполнен", oid);
                return Mono.empty();
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ Ошибка при перезапуске ресертификации, OID={}, статус={}, тело={}",
                                    oid, response.statusCode(), body);
                            return Mono.error(new ResponseStatusException(response.statusCode(), body));
                        });
            }
        });
    }

    public Mono<ResponseEntity<?>> startRemediationCampaign(UUID oid, String authHeader) {

        WebClient client = getClient(authHeader).build();

        return client.post().uri("/api/glt/certificationService/" + oid + "/startRemediation").exchangeToMono(response -> {
            if (response.statusCode().is2xxSuccessful()) {
                log.info("✅ Запуск процесса выполнения результатов ресертификации", oid);
                return Mono.empty();
            } else {
                return response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("❌ Ошибка при запуске процесса выполнения результатов ресертификации, OID={}, статус={}, тело={}",
                                    oid, response.statusCode(), body);
                            return Mono.error(new ResponseStatusException(response.statusCode(), body));
                        });
            }
        });
    }

    public ResponseEntity<Object> searchObject(String type, String authHeader, Object body,
            List<String> options,
            List<String> include,
            List<String> exclude,
            List<String> resolveNames) {
        WebClient client = getClient(authHeader).build();
        String uri = "/api/glt/" + type + "/search";

        try {
            WebClient.RequestBodySpec request = client.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path(uri);
                        if (options != null && !options.isEmpty()) {
                            uriBuilder.queryParam("options", String.join(",", options));
                        }
                        if (include != null && !include.isEmpty()) {
                            uriBuilder.queryParam("include", String.join(",", include));
                        }
                        if (exclude != null && !exclude.isEmpty()) {
                            uriBuilder.queryParam("exclude", String.join(",", exclude));
                        }
                        if (resolveNames != null && !resolveNames.isEmpty()) {
                            uriBuilder.queryParam("resolveNames", String.join(",", resolveNames));
                        }
                        return uriBuilder.build();
                    });

            return request
                    .bodyValue(body)
                    .retrieve()
                    .toEntity(Object.class).block();
        }
        catch (Exception ex){

            String cause = String.valueOf(ex.getCause());
            String message = "ошибка в: " + ex.getMessage() + " причина: " + ex.getCause();
            return ResponseEntity.ok(message);
        }
    }

    public ResponseEntity<Object> getObject(String type, String id, String authHeader,
        List<String> options,
        List<String> include,
        List<String> exclude,
        List<String> resolveNames) {
        RestTemplate restTemplate = new RestTemplate();

        String baseUrl = "http://localhost:8080/midpoint";

        String path = "/api/glt/" + type + "/" + id;

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl + path);

        if (options != null && !options.isEmpty()) {
            uriBuilder.queryParam("options", String.join(",", options));
        }
        if (include != null && !include.isEmpty()) {
            uriBuilder.queryParam("include", String.join(",", include));
        }
        if (exclude != null && !exclude.isEmpty()) {
            uriBuilder.queryParam("exclude", String.join(",", exclude));
        }
        if (resolveNames != null && !resolveNames.isEmpty()) {
            uriBuilder.queryParam("resolveNames", String.join(",", resolveNames));
        }

        String url = uriBuilder.toUriString();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Object> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Object.class
            );
            return response;
        } catch (Exception ex) {
            String cause = String.valueOf(ex.getCause());
            String message = "ошибка в: " + ex.getMessage() + " причина: " + cause;
            return ResponseEntity.ok(message);
    }
}

    public ResponseEntity<?> deleteDeputy(String authHeader, Object body) {
        WebClient client = getClient(authHeader).build();
        String uri = "/api/ws/rest/rpc/executeScript";
        try {
            WebClient.RequestBodySpec request = client.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(uri)
                            .build()
                    );

            return request
                    .bodyValue(body) // Передаете тело запроса
                    .retrieve()
                    .toEntity(Object.class)
                    .block();
        } catch (Exception ex) {
            String cause = String.valueOf(ex.getCause());
            String message = "ошибка в: " + ex.getMessage() + " причина: " + cause;
            return ResponseEntity.status(500).body(message);
        }
    }

    public ResponseEntity<String> proxyGetRequest(String param, HttpHeaders incomingHeaders, String authorizationHeader) {
        String path = "/api/glt";
        String fullUrl = midpointBaseUrl + path + param;
//        String fullUrl = "http://localhost:8080/midpoint" + path + param;
        log.info("Проксирую GET запрос на {}", fullUrl);

        int maxSizeInBytes = 10 * 1024 * 1024;

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(maxSizeInBytes))
                .build();

        return getClient(authorizationHeader)
                .exchangeStrategies(strategies)
                .build()
                .get() //.get()
                .uri(fullUrl) // .uri(path + param)
                .headers(httpHeaders -> {
                    httpHeaders.addAll(incomingHeaders);
                    httpHeaders.remove(HttpHeaders.HOST);
                })
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            log.info("MidPoint GET ответ: {} {}", response.statusCode(), body);
                            HttpHeaders responseHeaders = new HttpHeaders();
                            response.headers().asHttpHeaders().forEach((k, v) -> {
                                if (!"Access-Control-Allow-Origin".equalsIgnoreCase(k)
                                        && !"Transfer-Encoding".equalsIgnoreCase(k)) {
                                    responseHeaders.put(k, v);
                                }
                            });
                            return new ResponseEntity<>(body, responseHeaders, response.statusCode());
                        })
                )
                .block();
    }

    public ResponseEntity<String> proxyPostRequest(String param, String rawJsonBody, HttpHeaders incomingHeaders, String authorizationHeader) {
        String path = "/api/glt";
        String fullUrl = midpointBaseUrl + path + param;
        log.info("Проксирую POST запрос на {}", fullUrl);
        log.debug("Тело запроса:\n{}", rawJsonBody);

        int maxSizeInBytes = 10 * 1024 * 1024;

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(config -> config.defaultCodecs().maxInMemorySize(maxSizeInBytes))
                .build();

        WebClient client = getClient(authorizationHeader)
                .exchangeStrategies(strategies)
                .build();

        return client.post()
                .uri(path + param)
                .headers(headers -> {
                    headers.addAll(incomingHeaders);
                    headers.remove(HttpHeaders.HOST);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setContentLength(rawJsonBody.getBytes(StandardCharsets.UTF_8).length);
                })
                .bodyValue(rawJsonBody)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(body -> {
                            log.info("MidPoint POST ответ: {} {}", response.statusCode(), body);
                            HttpHeaders responseHeaders = new HttpHeaders();
                            response.headers().asHttpHeaders().forEach((k, v) -> {
                                if (!"Access-Control-Allow-Origin".equalsIgnoreCase(k)
                                        && !"Transfer-Encoding".equalsIgnoreCase(k)) {
                                    responseHeaders.put(k, v);
                                }
                            });
                            return new ResponseEntity<>(body, responseHeaders, response.statusCode());
                        })
                )
                .block();
    }
}
