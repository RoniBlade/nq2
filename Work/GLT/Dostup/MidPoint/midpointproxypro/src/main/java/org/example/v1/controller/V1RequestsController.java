package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1RequestsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1/api/")
@OpenAPIDefinition(info = @Info(title = "approval requests API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1ApprovalRequestController", description = "Взаимодействие с ")
@RequiredArgsConstructor
public class V1RequestsController {

    private final V1RequestsService service;

    @PostMapping("/request")
    public Page<Map<String, Object>> getAccounts(
            @RequestBody FilterRequest filters,
            Pageable pageable
    ){
        return service.getRequest(filters, pageable);
    }
}
