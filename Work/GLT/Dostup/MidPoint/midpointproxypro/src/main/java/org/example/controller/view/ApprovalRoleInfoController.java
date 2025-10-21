package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.ApprovalRoleInfoDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.ApprovalRoleInfoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/approval-role")
@RequiredArgsConstructor
@OpenAPIDefinition(info = @Info(title = "Approval role API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "ApprovalRoleInfoService", description = "Взаимодействие с информацией о связях ролей с объектами")
public class ApprovalRoleInfoController {
    final ApprovalRoleInfoService service;

    @GetMapping("/view")
    public Page<ApprovalRoleInfoDto> getApprovalRoleInfo(
            @RequestParam(name = "lang", required = false) String lang,
            Pageable pageable,
            Authentication authentication){
        log.info("[CTRL] GET /approval-role/view with lang {}", lang);
        return service.getApprovalRoleInfo(pageable, lang);
    }

    @PostMapping("/filtered-view")
    public Page<ApprovalRoleInfoDto> searchApprovalRoleInfo(
            @RequestBody FilterRequest request,
            @RequestParam(name = "lang", required = false) String lang,
            Pageable pageable,
            Authentication authentication){
        log.info("[CTRL] POST /approval-role/view with Filters {}", request.getFilters());
        return service.searchApprovalRoleInfo(
                request,
                pageable,
                lang
        );
    }
}
