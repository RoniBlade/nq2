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
import org.example.dto.view.DelegationDto;
import org.example.model.filter.FilterRequest;
import org.example.service.AccessCertCampaignService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Data
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/access-cert-campaign")
@OpenAPIDefinition(info = @Info(title = "Config param API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AccessCertCampaignController", description = "Взаимодействие с кампаниями, связанными с сертификатами доступа")
public class AccessCertCampaignController {

    private final AccessCertCampaignService service;

    @Operation(summary = "Получение всех кампаний")
    @GetMapping("/view")
    public Page<AccessCertCampaignDto> getCampaign(Pageable pageable,
            Authentication authentication) {
        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }

    @Operation(summary = "Получение кампаний по oid")
    @GetMapping("/view/{oid}")
    public Page<AccessCertCampaignDto> getCampaignByOid(Pageable pageable,
            Authentication authentication,
            @RequestParam(name = "lang", required = false) String lang,
            @PathVariable(name = "oid") UUID oid){
        log.info("[CTRL] GET /access-cert-campaign/view by Oid");
        return service.getCampaignByOid(pageable, oid);
    }

    @Operation(summary = "Получение компаний с фильтрацией")
    @PostMapping("/filtered-view")
    public Page<AccessCertCampaignDto> searchDelegations(@RequestBody FilterRequest request,
            Pageable pageable,
            Authentication authentication,
            @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /access-cert-campaign/view with Filters {}", request.getFilters());
        return service.searchCampaign(
                request,
                pageable
        );
    }

    @Operation(summary = "Переход на следующий этап кампании по oid")
    @GetMapping("/next-stage")
    public Page<AccessCertCampaignDto> nextStageCampaign(Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }

    @Operation(summary = "Завершить этап кампании по oid")
    @GetMapping("/close-stage")
    public Page<AccessCertCampaignDto> closeCurrentStageCampaign(Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }

    @Operation(summary = "Завершить ресертификацию по oid")
    @GetMapping("/close-campaign")
    public Page<AccessCertCampaignDto> closeCampaign(Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }

    @Operation(summary = "Перезапустить ресертификацию по oid")
    @GetMapping("/reiterate-campaign")
    public Page<AccessCertCampaignDto> reiterateCampaign(Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }

    @Operation(summary = "Запуск процессов выполнения результатов ресертификацию по oid")
    @GetMapping("/remediation-campaign")
    public Page<AccessCertCampaignDto> remediationCampaign(Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] GET /access-cert-campaign/view");
        return service.getCampaign(pageable);
    }
}
