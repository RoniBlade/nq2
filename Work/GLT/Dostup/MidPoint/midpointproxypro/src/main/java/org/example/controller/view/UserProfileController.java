package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.UserProfileDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.UserProfileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-profile")
@OpenAPIDefinition(info = @Info(title = "User profile API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "UserProfileController", description = "Взаимодействие с профилями пользователей")
public class UserProfileController {

    private final UserProfileService service;

    @GetMapping("/view")
    public Page<UserProfileDto> getUserProfile(Authentication authentication,
                                               Pageable pageable,
                                               @RequestParam(name = "lang", required = false) String lang) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] GET /user-profile/view by user {} with lang {}", userOid, lang);
        return service.getUserProfile(pageable, lang);
    }

    @PostMapping("/filtered-view")
    public Page<UserProfileDto> searchUserProfile(Authentication authentication,
                                                  Pageable pageable,
                                                  @RequestParam(name = "lang", required = false) String lang,
                                                  @RequestBody FilterRequest request) {
        String userOid = authentication.getPrincipal().toString();
        log.info("[CTRL] POST /user-profile/filtered-view by user {} with lang {}", userOid, lang);
        return service.searchUserProfile(request, pageable, lang);
    }
}
