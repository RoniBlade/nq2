package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.v1.service.V1CaseInfoService;
import org.example.v1.service.V1UserAssignmentsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1UserAssignmentsController", description = "Получение связей и назначений ролей и пользователей в системе")
public class V1CaseInfoController {

    private final V1CaseInfoService service;

    @PostMapping("/case-info/param/{param}")
    public Page<Map<String, Object>> getCaseInfo(
            Pageable pageable,
            @PathVariable Object param
    ){
        return service.getCaseInfo(pageable, param.toString());
    }
}
