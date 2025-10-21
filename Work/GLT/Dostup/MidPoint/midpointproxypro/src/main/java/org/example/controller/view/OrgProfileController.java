package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.OrgProfileDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.OrgProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/org-profile")
@OpenAPIDefinition(info = @Info(title = "Organization profile API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "OrgProfileController", description = "Взаимодействие с информацией о пользователе, включая стандартные атрибуты")
public class OrgProfileController {

    private final OrgProfileService service;

    @GetMapping("/view")
    public Page<OrgProfileDto> getOrgProfile(Authentication authentication,
            Pageable pageable,
            @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /org-profile/view with lang {}", lang);
        return service.getOrgProfile(pageable, lang);
    }

    @PostMapping("/filtered-view")
    public Page<OrgProfileDto> searchOrgProfile(Authentication authentication,
            Pageable pageable,
            @RequestParam(name = "lang", required = false) String lang,
            @RequestBody FilterRequest request) {
        log.info("[CTRL] POST /org-profile/filtered-view  with lang {}", lang);
        return service.searchOrgProfile(request, pageable, lang);
    }
}
