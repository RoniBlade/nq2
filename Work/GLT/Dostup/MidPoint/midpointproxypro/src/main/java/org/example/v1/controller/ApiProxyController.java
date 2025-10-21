package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.service.ApiProxyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/api/proxy")
@Slf4j
@OpenAPIDefinition(info = @Info(title = "proxy API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "ApiProxyController", description = "отправка запросов в мидпоинт")
@RequiredArgsConstructor
public class ApiProxyController {

    private final ApiProxyService service;

    @GetMapping()
    public ResponseEntity<String> proxyGet(
            @RequestParam("param") String urlParam,
            @RequestHeader HttpHeaders headers,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return service.proxyGetRequest(urlParam, headers, authorizationHeader);
    }


    @PostMapping("")
    public ResponseEntity<String> proxyPost(
            @RequestParam("param") String urlParam,
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders incomingHeaders,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return service.proxyPostRequest(urlParam, rawBody, incomingHeaders, authorizationHeader);
    }
}
