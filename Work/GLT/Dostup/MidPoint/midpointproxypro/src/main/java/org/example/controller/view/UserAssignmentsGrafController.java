package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.UserAssignmentsGrafDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.UserAssignmentsGrafService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/user-assignments-graf")
@OpenAPIDefinition(info = @Info(title = "User assignments API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "UserAssignmentsGrafController", description = "Взаимодействие с визуальными атрибутами архетипов")
public class UserAssignmentsGrafController {

    private final UserAssignmentsGrafService service;

    @GetMapping("/view")
    public Page<UserAssignmentsGrafDto> getUserAssignmentsGraf(Pageable pageable,
                                                               Authentication authentication,
                                                               @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] GET /user-assignments-graf/view by user {} with lang {}", userOid, lang);
        return service.getUserAssignmentsGraf(pageable, lang);
    }

    @PostMapping("/filtered-view")
    public Page<UserAssignmentsGrafDto> searchUserAssignmentsGraf(Pageable pageable,
                                                                  Authentication authentication,
                                                                  @RequestParam(name = "lang", required = false) String lang,
                                                                  @RequestBody FilterRequest request) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /user-assignments-graf/filtered-view by user {} with lang {}", userOid, lang);
        return service.searchUserAssignmentsGraf(request, pageable, lang);
    }
}
