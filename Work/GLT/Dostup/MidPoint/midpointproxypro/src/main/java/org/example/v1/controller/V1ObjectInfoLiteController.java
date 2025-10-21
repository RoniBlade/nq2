package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1AccountAssociationService;
import org.example.v1.service.V1ObjectInfoLiteService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1ObjectInfoLiteController", description = "Взаимодействие с информацией о пользователе, включая стандартные атрибуты")
public class V1ObjectInfoLiteController {

    private final V1ObjectInfoLiteService service;

    @PostMapping("/object-info-lite/param/{param}")
    public Page<Map<String, Object>> getObjectInfoLite(@PathVariable Object param, Pageable pageable){
        return service.getObjectInfoLite(param.toString(), pageable);
    }
}
