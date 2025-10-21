package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1CountRequestService;
import org.example.v1.service.V1DelegationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1/api/")
@OpenAPIDefinition(info = @Info(title = "delegations API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1CertAndQueryController", description = "получение связей для сертификаций и запросов")
@RequiredArgsConstructor
public class V1CountRequestController {

    private final V1CountRequestService service;

    @PostMapping("/count-request/param/{param}")
    public Page<Map<String, Object>> getCountRequest(
            Pageable pageable,
            @PathVariable String param
    ){
        return service.getCountRequest(pageable, param);
    }
}
