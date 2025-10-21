package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1AccountsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1AccountsController", description = "Получение набора атрибутов для каждого аккаунта")
public class V1AccountsController {

    private final V1AccountsService service;

    @PostMapping("/accounts/param/{param}")
    public Page<Map<String, Object>> getAccounts(
            @PathVariable Object param,
            @RequestBody FilterRequest filters,
            Pageable pageable){
        return service.getAccounts(pageable, param.toString(), filters);
    }
}
