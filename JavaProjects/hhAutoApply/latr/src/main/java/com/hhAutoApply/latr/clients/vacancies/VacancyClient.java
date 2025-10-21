package com.hhAutoApply.latr.clients.vacancies;

import com.hhAutoApply.latr.config.AppProperties;
import com.hhAutoApply.latr.exceptions.VacancyException;
import com.hhAutoApply.latr.models.Vacancy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class VacancyClient {


    private final AppProperties appProperties;
    private final WebClient serviceWebClient;

    public List<Vacancy> fetchVacancies(String accessToken, Map<String, String> params) {

        return serviceWebClient.get()
                .uri(buildUrl(params))
                .headers(httpHeaders -> {
                    setDefaultHeaders(httpHeaders, accessToken);
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                        Mono.error(new VacancyException("Ошибка клиента: " + response.statusCode()))
                )
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                        Mono.error(new VacancyException("Ошибка сервера: " + response.statusCode()))
                )
                .bodyToMono(String.class)
                .retry(2)
                .map(this::parseVacancies)
                .onErrorResume(e -> {
                    log.error("Ошибка при получении вакансий", e);
                    throw new VacancyException("Ошибка при получении вакансий: " + e.getMessage() );
                })
                .block();

    }


    private void setDefaultHeaders(HttpHeaders httpHeaders, String accessToken) {
        httpHeaders.setBearerAuth(accessToken);
        httpHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/119.0.0.0 Safari/537.36");
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
    }


    String buildUrl(Map<String, String> params) {
        String baseUrl = appProperties.getSearch_url();
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(baseUrl).append("?");
        try {
            urlBuilder.append("text=").append(URLEncoder.encode(params.get("text"), "UTF-8"));
            urlBuilder.append("&area=").append(URLEncoder.encode(params.get("area"), "UTF-8"));
            urlBuilder.append("&experience=").append(URLEncoder.encode(params.get("experience"), "UTF-8"));
            urlBuilder.append("&per_page=").append(URLEncoder.encode(params.get("per_page"), "UTF-8"));
            urlBuilder.append("&page=").append(URLEncoder.encode(params.get("page"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new VacancyException("Ошибка кодирования URL: " + e.getMessage());
        }

        return urlBuilder.toString();

    }

    private List<Vacancy> parseVacancies(String body) {
        JSONObject jsonObject = new JSONObject(body);
        if (!jsonObject.has("items") || jsonObject.isEmpty()) {
            throw new VacancyException("Ошибка при получении вакансий - ответ пустой");
        }

        JSONArray items = jsonObject.getJSONArray("items");
        List<Vacancy> vacancies = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject vacancyJson = items.getJSONObject(i);
            vacancies.add(parseVacancy(vacancyJson));
        }
        return vacancies;
    }

    Vacancy parseVacancy(JSONObject itemJson) {
        Vacancy vacancy = new Vacancy();
        vacancy.setId(Long.valueOf(itemJson.optString("id", null)));
        vacancy.setName(itemJson.optString("name", null));
        return vacancy;
    }

    public void applyVacancy(String accessToken, Long id) {
        String uri = appProperties.getApply_url();
        log.info("📌 Отправка отклика по URL: {}", uri);

        MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
        formData.add("resume_id", appProperties.getResumeId());
        formData.add("vacancy_id", id.toString());
        formData.add("message", appProperties.getMessage());

        serviceWebClient.post()
                .uri(uri)
                .headers(httpHeaders -> {
                    httpHeaders.setBearerAuth(accessToken);
                    httpHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/119.0.0.0 Safari/537.36");
                    httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
                })
                .body(BodyInserters.fromMultipartData(formData))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("❌ Ошибка запроса: статус={} ответ={}", clientResponse.statusCode(), errorBody);

                            if (errorBody.contains("Already applied")) {
                                log.warn("⚠️ Уже откликнулись на вакансию (ID: {}). Пропускаем...", id);
                                return Mono.empty();
                            }

                            return Mono.error(new VacancyException("Ошибка сервера: " + clientResponse.statusCode() + " -> " + errorBody));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("❌ Ошибка сервера: статус={} ответ={}", clientResponse.statusCode(), errorBody);
                            return Mono.error(new VacancyException("Ошибка сервера: " + clientResponse.statusCode() + " -> " + errorBody));
                        })
                )
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("✅ Отклик на вакансию {} успешен", id))
                .onErrorResume(error -> {
                    log.warn("⚠️ Ошибка обработана, продолжаем выполнение. Причина: {}", error.getMessage());
                    return Mono.empty();
                })
                .block();


    }



}
