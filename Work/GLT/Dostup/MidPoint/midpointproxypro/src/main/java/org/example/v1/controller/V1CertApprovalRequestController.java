package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1CertApprovalRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Certification approval request API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1CertApprovalRequestController", description = "Взаимодействие с актуальными запросами на предоставление доступа")
public class V1CertApprovalRequestController {

    private final V1CertApprovalRequestService service;

    @PostMapping("/cert-request")
    public Page<Map<String, Object>> getCertApproval(
            Pageable pageable,
            @RequestBody FilterRequest filters
    ){
        return service.getCertApprovalRequest(filters, pageable);
    }
}
