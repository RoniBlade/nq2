package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.v1.dto.DMenuParamDto;
import org.example.dto.view.MenuParamDto;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.MenuParamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("v1/api/menu-param")
@OpenAPIDefinition(info = @Info(title = "Menu param API", version = "v2"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "MenuParamController", description = "Взаимодействие со списком всех параметров меню с их основными свойствами")
public class MenuParamDefinitionsController {

    private final MenuParamService service;

    @GetMapping("/")
    public Page<DMenuParamDto> getMenu(Pageable pageable, Authentication authentication) {
        log.info("[CTRL] GET /menu-param/");
        return service.getMenu(pageable);
    }

    @PostMapping("/filtered")
    public Page<DMenuParamDto> searchMenu(@RequestBody FilterRequest request,
                                         Pageable pageable,
                                         Authentication authentication) {
        log.info("[CTRL] POST /menu-param/filtered");
        return service.searchMenu(request, pageable);
    }

    @PostMapping
    public ResponseEntity<DMenuParamDto> createMenuParam(@RequestBody DMenuParamDto dto) {
        log.info("[CTRL] POST /config-params with body={}", dto);
        DMenuParamDto created = service.createMenuParam(dto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{oid}")
    public ResponseEntity<DMenuParamDto> updateMenuParam(
            @PathVariable("oid") UUID oid,
            @RequestBody DMenuParamDto dto) {
        log.info("[CTRL] PUT /config-params/{} with body={}", oid, dto);
        try {
            DMenuParamDto updated = service.updateMenuParam(oid, dto);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException ex) {
            log.warn("[CTRL] PUT /config-params/{} not found: {}", oid, ex.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{oid}")
    public ResponseEntity<?> deleteMenuParam(@PathVariable UUID oid) {
        log.info("[CTRL] DELETE /menu-param/{}", oid);
        try {
            service.deleteMenuParam(oid);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException ex) {
            log.warn("[CTRL] DELETE /menu-param/{} not found: {}", oid, ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "MenuParamField not found", "oid", oid));
        }
    }

    @PostMapping("/data/{oid}")
    public Page<Map<String, Object>> searchDataFromMenuByOid(@PathVariable String oid,
                                                             @RequestBody(required = false) FilterRequest request,
                                                             Pageable pageable,
                                                             Authentication authentication) {
        return service.searchDataFromMenuByOid(oid, request, pageable);
    }

}
