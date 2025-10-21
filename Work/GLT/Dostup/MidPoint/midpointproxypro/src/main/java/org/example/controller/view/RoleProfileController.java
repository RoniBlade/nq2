package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.OrgProfileDto;
import org.example.dto.view.RoleProfileDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.OrgProfileService;
import org.example.service.view.RoleProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/role-profile")
@OpenAPIDefinition(info = @Info(title = "role profile API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "RoleProfileController", description = "Взаимодействие с информацией о ролях, включая стандартные атрибуты")
public class RoleProfileController {

    private final RoleProfileService service;

    @GetMapping("/view")
    public Page<RoleProfileDto> getRoleProfile(Authentication authentication,
            Pageable pageable) {
        log.info("[CTRL] GET /role-profile/view");
        return service.getRoleProfile(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<RoleProfileDto> searchRoleProfile(Authentication authentication,
            Pageable pageable,
            @RequestBody FilterRequest request) {
        log.info("[CTRL] POST /role-profile/filtered-view");
        return service.searchRoleProfile(request, pageable);
    }
}
