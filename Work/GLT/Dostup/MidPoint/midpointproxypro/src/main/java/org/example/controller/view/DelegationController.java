package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.DelegationDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.DelegationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/delegations")
@OpenAPIDefinition(info = @Info(title = "Delegation API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "DelegationController", description = "Взаимодействие с текущими или актуальными делегированием полномочий"
        + " между пользователями")
public class DelegationController {

    private final DelegationService service;

    public DelegationController(DelegationService service) {
        this.service = service;
    }

    @GetMapping("/view")
    public Page<DelegationDto> getDelegations(Pageable pageable,
                                              Authentication authentication,
                                              @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /delegations/view with lang {}", lang);
        return service.getDelegationViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<DelegationDto> searchDelegations(@RequestBody FilterRequest request,
                                                           Pageable pageable,
                                                           Authentication authentication,
                                                           @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /delegations/view with Filters {}", request.getFilters());
        return service.searchDelegationViaJdbc(
                request,
                pageable
        );
    }
}
