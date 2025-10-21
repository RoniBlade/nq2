package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.v1.service.V1AccountAttributeService;
import org.example.v1.service.V1UserAssignmentsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1AccountsAttributeController", description = "Получение атрибутов объектов с учётом прав доступа")
public class V1AccountsAttributeController {

    private final V1AccountAttributeService service;

    @PostMapping("/account-attribute/param/{param}")
    public Page<Map<String, Object>> getAccountAttribute(
            Pageable pageable,
            @PathVariable Object param
    ){
        return service.getAccountAttribute(pageable, param.toString());
    }
}
