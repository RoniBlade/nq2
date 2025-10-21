package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.CaseDto;
import org.example.dto.view.CaseInfoDto;
import org.example.model.filter.FilterRequest;
import org.example.service.CaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RequestMapping("/api/case-info")
@RestController
@RequiredArgsConstructor
@OpenAPIDefinition(info = @Info(title = "Case info API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "CaseInfoController", description = "Взаимодействие с информацией (роли, пользователь, организация)")
public class CaseInfoController {

    private final CaseService service;

    @GetMapping("/view")
    public Page<CaseInfoDto> getCaseInfo(Pageable pageable,
                                         Authentication authentication,
                                         @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] GET /case-info/view by user {} with lang {}", userOid, lang);
        return service.getCaseInfo(pageable, lang);
    }

    @PostMapping("/filtered-view")
    public Page<CaseInfoDto> searchCaseInfo(@RequestBody FilterRequest request,
                                                    Pageable pageable,
                                                    Authentication authentication,
                                                    @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /case-info/filtered-view by user {} with lang {}", userOid, lang);
        return service.searchCaseInfo(
                request,
                pageable,
                lang
        );
    }

    @Operation(summary = "Получение case с фильтром")
    @PostMapping("/cases/search")
    public Page<CaseDto> searchCase(@RequestBody FilterRequest request,
                                    Pageable pageable,
                                    Authentication authentication,
                                    @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /cases/view with Filters {}", request.getFilters());
        return service.searchCases(
                request,
                pageable
        );
    }

    @Operation(summary = "получение case по Oid")
    @GetMapping("/cases/{oid}")
    public Page<CaseDto> getCaseByOid(Pageable pageable,
            Authentication authentication,
            @PathVariable("oid") UUID oid){
        return service.getCaseByOid(pageable, oid);
    }

    @Operation(summary = "получение case по Name")
    @PostMapping("/cases/view")
    public Page<CaseDto> getCaseByOid(Pageable pageable,
            Authentication authentication,
            @RequestBody String nameOrig){
        System.out.println(nameOrig);
        return service.getCaseByName(pageable, nameOrig);
    }

}
