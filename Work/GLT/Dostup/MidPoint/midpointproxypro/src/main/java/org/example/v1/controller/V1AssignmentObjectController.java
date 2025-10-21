package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1AssignmentObjectService;
import org.example.v1.service.V1CertApprovalRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object assignment API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1AssignmentObjectController", description = "Описывает информацию о пользовательском объекте в системе")
public class V1AssignmentObjectController {

    private final V1AssignmentObjectService service;

    @PostMapping("/assignment-object/param/{param}")
    public Page<Map<String, Object>> getAssignmentObject(
            Pageable pageable,
            @RequestBody FilterRequest filters,
            @PathVariable String param
    ){
        return service.getObjectAssignmentRequest(filters, pageable, param);
    }

    @PostMapping("/assignment-object")
    public Page<Map<String, Object>> getAssignmentObject(
            Pageable pageable,
            @RequestBody FilterRequest filters
    ){
        return service.getObjectAssignmentRequest(filters, pageable, null);
    }
}
