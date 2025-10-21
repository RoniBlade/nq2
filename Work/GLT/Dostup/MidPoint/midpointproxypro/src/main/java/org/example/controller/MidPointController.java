package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.net.openssl.ciphers.Authentication;
import org.example.service.MidpointService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/mid")
@OpenAPIDefinition(info = @Info(title = "Midpoint API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "MidpointController", description = "Взаимодействие с rest сервисами из Midpoint")
public class MidPointController {

    private final MidpointService service;

    @Operation(summary = "Получение объектов")
    @GetMapping(value = "{type}/search/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getObject(
            @PathVariable("type") String type,
            @PathVariable("id") String id,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {

        Object xmlContent = service.getObject(id, authorizationHeader, type, options, include, exclude, resolveNames);
        return ResponseEntity.ok()

                .header(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_XML_VALUE))
                .body(xmlContent);

    }

    @Operation(summary = "Получение объектов с фильтрами")
    @PostMapping("{type}/search")
    public ResponseEntity<Object> getFilteredObject(
            @PathVariable("type") String type,
            @RequestBody Object body,
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(value = "options", required = false) List<String> options,
            @RequestParam(value = "include", required = false) List<String> include,
            @RequestParam(value = "exclude", required = false) List<String> exclude,
            @RequestParam(value = "resolveNames", required = false) List<String> resolveNames) {


        Object xmlContent = service.getFilteredObject(body, authorizationHeader, type, options, include, exclude, resolveNames);
        return ResponseEntity.ok()
                .body(xmlContent);
    }

    @Operation(summary = "Перейти на следующий этап ресертификации")
    @PostMapping("/{oid}/open-next-stage")
    public Mono<ResponseEntity<?>> OpenNextStage(
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader,
            Authentication authentication
    ) {
        log.info("🔄 Запроc на переход на следующий этап ресертифкации с oid={}", oid);
        return service.openNextStage(authorizationHeader, oid);
    }

    @Operation(summary = "Завершить текущий этап ресертификации")
    @PostMapping("/{oid}/close-current-stage")
    public Mono<ResponseEntity<?>> CloseCurrentStage(
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("🔄 Запроc на завершение этапа ресертифкации с oid={}", oid);
        return service.closeCurrentStage(authorizationHeader, oid);
    }

    @Operation(summary = "Завершить ресертификацию")
    @PostMapping("/{oid}/close-campaign")
    public Mono<ResponseEntity<?>> closeCampaign(
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("🔄 Запроc на завершение ресертифкации с oid={}", oid);
        return service.closeCampaign(authorizationHeader, oid);
    }

    @Operation(summary = "Перезапустить ресертификацию")
    @PostMapping("/{oid}/reiterate-campaign")
    public Mono<ResponseEntity<?>> reiterateCampaign(
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        log.info("🔄 Запроc на перезапуск ресертифкации с oid={}", oid);
        return service.reiterateCampaign(authorizationHeader, oid);
    }

    @Operation(summary = "Запустить процесс выполнения результатов ресертификации")
    @PostMapping("/{oid}/start-remediation")
    public Mono<ResponseEntity<?>> startRemediation(
            @PathVariable UUID oid,
            @RequestHeader("Authorization") String authorizationHeader,
            Authentication authentication
    ) {
        log.info("🔄 Запроc на запуск процесса выполнения результатов ресертификации с oid={}", oid);
        return service.startRemediation(authorizationHeader, oid);
    }

    @Operation(summary = "удаление Deputy")
    @PostMapping("deputy/delete")
    public ResponseEntity<?> deleteDeputy(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody Object body
    ) {
        log.info("🔄 Запроc на удаление Deputy");
        return service.deleteDeputy(authorizationHeader, body);
    }
}
