package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.CertificationAndQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cert-query")
@RequiredArgsConstructor
@Slf4j
@OpenAPIDefinition(info = @Info(title = "Certification And Query API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "CertificationAndQueryController", description = "получение связей для сертификаций и запросов")
public class CertificationAndQueryController {

    private final CertificationAndQueryService service;

    @GetMapping("/view/{oid}")
    public Page<Map<String, Object>> getCertAndQuery(
            Authentication authentication,
            Pageable pageable,
            @PathVariable UUID oid) {
        log.info("[CTRL] GET /certAndQuery/byOid");
        return service.getAllByOid(pageable, oid);
    }
}
