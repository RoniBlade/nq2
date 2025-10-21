package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.v1.service.V1UserAssignmentsGrafService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Persistable;
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
@Tag(name = "V1UserAssignmentsGrafController", description = "Получение связи между объектом-владельцем и целевым объектом с определённым типом и архетипом")
public class V1UserAssignmentsGrafController {

    private final V1UserAssignmentsGrafService service;

    @PostMapping("/user-assignments-graf/param/{param}")
    public Page<Map<String, Object>> getAssignmentsGraf(
            Pageable pageable,
            @PathVariable Object param
    ){
        return service.getUserAssignmentsGraf(pageable, param.toString());
    }
}
