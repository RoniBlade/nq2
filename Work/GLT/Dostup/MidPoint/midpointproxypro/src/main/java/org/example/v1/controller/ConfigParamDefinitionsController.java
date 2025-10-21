package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.dto.DConfigParamDto;
import org.example.dto.view.ConfigParamDto;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.ConfigParamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("v1/api/config-params")
@OpenAPIDefinition(info = @Info(title = "Config param API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "ConfigParamController", description = "Взаимодействие с конфигурационными параметрами")
public class ConfigParamDefinitionsController {

    private final ConfigParamService service;

    @GetMapping("/")
    public Page<DConfigParamDto> getConfigParams(Pageable pageable,
                                                Authentication authentication,
                                                @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /config-params/view with lang={}", lang);
        return service.getConfigParams(pageable);
    }

    @PostMapping("/filtered")
    public Page<DConfigParamDto> searchConfigParams(@RequestBody FilterRequest request,
                                                   Pageable pageable,
                                                   Authentication authentication,
                                                   @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /config-params/filtered-view with filters={}", request.getFilters());
        return service.searchConfigParams(request, pageable);
    }

    @PostMapping
    public ResponseEntity<DConfigParamDto> createConfigParam(@RequestBody DConfigParamDto dto) {
        log.info("[CTRL] POST /config-params with body={}", dto);
        DConfigParamDto created = service.createConfigParam(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{oid}")
    public ResponseEntity<DConfigParamDto> updateConfigParam(
            @PathVariable("oid") UUID oid,
            @RequestBody DConfigParamDto dto) {
        log.info("[CTRL] PUT /config-params/{} with body={}", oid, dto);
        try {
            DConfigParamDto updated = service.updateConfigParam(oid, dto);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException ex) {
            log.warn("[CTRL] PUT /config-params/{} not found: {}", oid, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{oid}")
    public ResponseEntity<?> deleteConfigParam(@PathVariable("oid") UUID oid) {
        log.info("[CTRL] DELETE /config-params/{}", oid);
        try {
            service.deleteConfigParam(oid);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            log.warn("[CTRL] DELETE /config-params/{} not found: {}", oid, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "ConfigParamField not found", "oid", oid));
        }
    }

}
