// src/main/java/org/example/controller/view/AccountAttributeController.java
package org.example.controller.view;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.view.AccountAttributeDto;
import org.example.model.filter.FilterRequest;
import org.example.service.view.AccountAttributeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/account-attributes")
@OpenAPIDefinition(info = @Info(title = "Account attributes API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AccountAttributeController", description = "Взаимодействие с атрибутами объектов из таблицы shadow")
public class AccountAttributeController {

    private final AccountAttributeService service;

    public AccountAttributeController(AccountAttributeService service) {
        this.service = service;
    }

    @GetMapping("/view")
    public Page<AccountAttributeDto> getAccountAttributes(Pageable pageable,
                                                          Authentication auth,
                                                          @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] GET /api/account-attributes/view lang={}", lang);
        return service.getAccountAttributeViaJdbc(pageable);
    }

    @PostMapping("/filtered-view")
    public Page<AccountAttributeDto> searchAccountAttributes(@RequestBody FilterRequest request,
                                                             Pageable pageable,
                                                             Authentication auth,
                                                             @RequestParam(name = "lang", required = false) String lang) {
        log.info("[CTRL] POST /api/account-attributes/filtered-view filters={} fields={}",
                request.getFilters(), request.getFields());
        return service.searchAccountAttributeViaJdbc(
                request,
                pageable
        );
    }
}
