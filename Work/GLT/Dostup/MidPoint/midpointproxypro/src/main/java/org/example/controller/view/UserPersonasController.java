package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.UserPersonasDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.UserPersonasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-personas")
@OpenAPIDefinition(info = @Info(title = "User profile API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "UserPersonasController", description = "Взаимодействие со связями персоны между пользователями системы")
public class UserPersonasController {

    private final UserPersonasService service;

    @GetMapping("/view")
    public Page<UserPersonasDto> getUserPersonas(Authentication authentication, Pageable pageable) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] GET /user-personas/view by user {}", userOid);
        return service.getUserPersonasViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<UserPersonasDto> searchUserPersonas(Authentication authentication,
                                                    Pageable pageable,
                                                    @RequestBody FilterRequest request) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /user-personas/filtered-view by user {}", userOid);
        return service.searchUserPersonasViaJdbc(request, pageable);
    }
}
