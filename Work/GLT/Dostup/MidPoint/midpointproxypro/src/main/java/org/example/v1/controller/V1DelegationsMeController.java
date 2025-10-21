package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1DelegationService;
import org.example.v1.service.V1DelegationsMeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1/api/")
@OpenAPIDefinition(info = @Info(title = "delegations API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1DelegationController", description = "Взаимодействие с текущими или актуальными делегированием полномочий")
@RequiredArgsConstructor
public class V1DelegationsMeController {

    private final V1DelegationsMeService service;

    @PostMapping("/delegations-me")
    public Page<Map<String, Object>> getDelegationMe(
            Pageable pageable,
            @RequestBody FilterRequest filters
    ){
        return service.getDelegationsMe(filters, pageable);
    }
}
