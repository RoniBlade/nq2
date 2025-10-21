package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1PersonasService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/")
@OpenAPIDefinition(info = @Info(title = "User profile API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1UserPersonasController", description = "Взаимодействие со связями персоны между пользователями системы")
public class V1PersonasController {

    private final V1PersonasService service;

    @PostMapping("/personas/param/{param}")
    public Page<Map<String, Object>> getCertApproval(
            @PathVariable("param") Object param,
            Pageable pageable,
            @RequestBody FilterRequest filters
    ){
        return service.getPersonas(filters, pageable, param.toString());
    }
}
