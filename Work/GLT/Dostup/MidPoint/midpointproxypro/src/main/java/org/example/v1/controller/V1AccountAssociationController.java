package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.V1AccountAssociationService;
import org.example.v1.service.V1AccountAttributeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api")
@OpenAPIDefinition(info = @Info(title = "Object API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "V1AccountAssociationController", description = "Получение ассоциированных данных аккаунтов")
public class V1AccountAssociationController {

    private final V1AccountAssociationService service;

    @PostMapping("/account-association/intent/{intent}/param/{param}")
    public Page<Map<String, Object>> getAccountAssociation(
            @PathVariable Object intent,
            Pageable pageable,
            @PathVariable Object param,
            @RequestBody FilterRequest filters
    ){
        System.out.println();
        return service.getAccountAssociation(pageable, param.toString(), filters, intent.toString());
    }
//
//    @PostMapping("/account-association-test/param/{param}")
//    public Page<Map<String, Object>> getAccountAssociationTest(
//            Pageable pageable,
//            @PathVariable Object param
//    ){
//        return service.getAccountAssociationTest(pageable, param.toString());
//    }

//    @PostMapping("/account-association/param/{param}")
//    public Page<Map<String, Object>> getAccountAssociation(
//            Pageable pageable,
//            @PathVariable Object param
//    ){
//        return service.getAccountAssociation(pageable, param.toString());
//    }
}
