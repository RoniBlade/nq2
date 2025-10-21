package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AccessCertCampaignDto;
import org.example.dto.AccessCertDefinitionDto;
import org.example.model.filter.FilterRequest;
import org.example.service.AccessCertCampaignService;
import org.example.service.AccessCertDefinitionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Data
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/access-cert-definition")
@OpenAPIDefinition(info = @Info(title = "Config param API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AccessCertDefinitionController", description = "Описание метаданных или структур определений сертификатов")
public class AccessCertDefinitionController {

    private final AccessCertDefinitionService service;

//    @Operation(summary = "Получение всех кампаний")
//    @GetMapping("/view")
//    public Page<AccessCertDefinitionDto> getCampaign(Pageable pageable,
//            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-definition/view");
//        return service.getDefinition(pageable);
//    }
//
//    @Operation(summary = "Получение кампаний по oid")
//    @GetMapping("/view/{oid}")
//    public Page<AccessCertDefinitionDto> getCampaignByOid(Pageable pageable,
//            Authentication authentication,
//            @RequestParam(name = "lang", required = false) String lang,
//            @PathVariable(name = "oid") UUID oid){
//        log.info("[CTRL] GET /access-cert-definition/view by Oid");
//        return service.getDefinitionByOid(pageable, oid);
//    }
//
//    @Operation(summary = "Получение компаний с фильтрацией")
//    @PostMapping("/filtered-view")
//    public Page<AccessCertDefinitionDto> searchDelegations(@RequestBody FilterRequest request,
//            Pageable pageable,
//            Authentication authentication,
//            @RequestParam(name = "lang", required = false) String lang) {
//        log.info("[CTRL] POST /access-cert-definition/view with Filters {}", request.getFilters());
//        return service.searchDefinition(
//                request,
//                pageable
//        );
//    }
}
