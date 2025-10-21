package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.RoleDto;
import org.example.dto.view.CaseInfoDto;
import org.example.model.filter.FilterRequest;
import org.example.service.RoleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/role")
@Slf4j
@OpenAPIDefinition(info = @Info(title = "Delegation API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "RoleController", description = "Получение информации о ролях")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService service;

    @GetMapping("/view")
    public Page<RoleDto> getCaseInfo(Pageable pageable, Authentication authentication) {
        log.info("[CTRL] GET /role/view");
        return service.getRole(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<RoleDto> searchCaseInfo(@RequestBody FilterRequest request,
            Pageable pageable,
            Authentication authentication) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /role/filtered-view");
        return service.searchRole(
                request,
                pageable
        );
    }
}
