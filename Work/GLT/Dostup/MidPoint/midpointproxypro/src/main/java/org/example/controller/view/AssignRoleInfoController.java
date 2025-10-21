package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.AssignRoleInfoDto;
import org.example.dto.view.DelegationDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.AssignRoleInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assign-role-info")
@Slf4j
@OpenAPIDefinition(info = @Info(title = "Delegation API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AssignRoleInfoController", description = "содержит информацию о связях или назначениях ролей между объектами")
@RequiredArgsConstructor
public class AssignRoleInfoController {

    private final AssignRoleInfoService service;

    @GetMapping("/view")
    public Page<AssignRoleInfoDto> getAssignRoleInfo(Pageable pageable,
            Authentication authentication) {
        log.info("[CTRL] GET /AssignRoleInfo/view");
        return service.getAssignRoleViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<AssignRoleInfoDto> searchAssignRoleInfo(@RequestBody FilterRequest request,
            Pageable pageable,
            Authentication authentication) {
//        log.info("[CTRL] POST /delegations/view with Filters {}", request.getFilters());
        return service.searchAssignRoleViaJdbc(
                request,
                pageable
        );
    }
}
