package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.OrgProfileExtDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.OrgProfileExtService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/org-profile-ext")
@OpenAPIDefinition(info = @Info(title = "Organization profile ext API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "OrgProfileExtController", description = "Взаимодействие со списком расширенных атрибутов каждого пользователя")
public class OrgProfileExtController {

    private final OrgProfileExtService service;

    @GetMapping("/view")
    public Page<OrgProfileExtDto> getOrgProfileExt(Authentication authentication,
                                                   Pageable pageable) {
        log.info("[CTRL] GET /org-profile-ext/view");
        return service.getOrgProfileExtViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<OrgProfileExtDto> searchOrgProfileExt(Authentication authentication,
                                                      Pageable pageable,
                                                      @RequestBody FilterRequest request) {
        log.info("[CTRL] POST /org-profile-ext/filtered-view");
        return service.searchOrgProfileExtViaJdbc(request, pageable);
    }
}
