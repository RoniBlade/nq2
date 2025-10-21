package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.RoleProfileDto;
import org.example.dto.view.RoleProfileExtDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.RoleProfileExtService;
import org.example.service.view.RoleProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/role-profile-ext")
@OpenAPIDefinition(info = @Info(title = "role profile ext API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "RoleProfileExtController", description = "Взаимодействие с информацией о ролях, включая стандартные атрибуты")
public class RoleProfileExtController {

    private final RoleProfileExtService service;

    @GetMapping("/view")
    public Page<RoleProfileExtDto> getRoleProfileExt(Authentication authentication,
            Pageable pageable,
            @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /role-profile-ext/view");
        return service.getRoleProfileExt(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<RoleProfileExtDto> searchRoleProfileExt(Authentication authentication,
            Pageable pageable,
            @RequestBody FilterRequest request) {
        log.info("[CTRL] POST /role-profile-ext/filtered-view");
        return service.searchRoleProfileExt(request, pageable);
    }
}
