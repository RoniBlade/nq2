package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.TestX5Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test_x5")
@Slf4j
@OpenAPIDefinition(info = @Info(title = "Delegation API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "TestX5Controller", description = "Получение информации о ролях")
@RequiredArgsConstructor
public class TestX5Controller {

    private final TestX5Service testX5Service;

    @GetMapping
    public ResponseEntity<String> proxyGet(
            @RequestParam("param") String urlParam,
            @RequestHeader HttpHeaders headers,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return testX5Service.proxyGetRequest(urlParam, headers, authorizationHeader);
    }

    @PostMapping
    public ResponseEntity<String> proxyPost(
            @RequestParam("param") String urlParam,
            @RequestBody String rawBody,
            @RequestHeader HttpHeaders incomingHeaders,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        return testX5Service.proxyPostRequest(urlParam, rawBody, incomingHeaders, authorizationHeader);
    }

}
