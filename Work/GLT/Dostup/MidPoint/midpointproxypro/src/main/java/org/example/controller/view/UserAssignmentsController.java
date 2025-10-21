package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.UserAssignmentsDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.UserAssignmentsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-assignments")
@Slf4j
@OpenAPIDefinition(info = @Info(title = "User assignments API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "UserAssignmentsController", description = "Взаимодействие с назначениями")
public class UserAssignmentsController {

    private final UserAssignmentsService service;

    @GetMapping("/view")
    public Page<UserAssignmentsDto> getUserAssignments(Pageable pageable,
                                                Authentication authentication) {
        log.info("[CTRL] GET /user-assignments/view");
        return service.getUserAssignmentsViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<UserAssignmentsDto> searchUserAssignments(@RequestBody FilterRequest request,
                                                   Pageable pageable,
                                                   Authentication authentication) {
        log.info("[CTRL] POST /user-assignments/filtered-view");
        return service.searchUserAssignmentsViaJdbc(
                request,
                pageable
        );
    }
}
