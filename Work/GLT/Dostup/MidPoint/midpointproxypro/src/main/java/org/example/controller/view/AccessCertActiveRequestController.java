package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.AccessCertActiveRequestDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.AccessCertActiveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cert-requests")
@OpenAPIDefinition(info = @Info(title = "Certification requests API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AccessCertActiveRequestController", description = "Взаимодействие с актуальными запросами на предоставление доступа"
        + " в системе сертификации доступа")
public class AccessCertActiveRequestController {

    private final AccessCertActiveRequestService service;

    public AccessCertActiveRequestController(AccessCertActiveRequestService service) {
        this.service = service;
    }

    @GetMapping("/view")
    public Page<AccessCertActiveRequestDto> getAccessCertActiveRequest(Pageable pageable,
                                                        Authentication authentication,
                                                        @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /cert-requests/view with lang {}", lang);
        return service.getMyRequestViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<AccessCertActiveRequestDto> searchAccessCertActiveRequest(@RequestBody FilterRequest request,
                                                           Pageable pageable,
                                                           Authentication authentication,
                                                           @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /cert-requests/view with Filters {}", request.getFilters());
        return service.searchMyRequestViaJdbc(
                request,
                pageable,
                lang
        );
    }
}
