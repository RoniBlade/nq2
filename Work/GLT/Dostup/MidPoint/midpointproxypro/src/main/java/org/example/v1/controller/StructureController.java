// StructureController.java
package org.example.v1.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.filter.FilterRequest;
import org.example.v1.service.StructureService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/structure")
@OpenAPIDefinition(info = @Info(title = "Structure API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "StructureController", description = "Взаимодействие с структурами данных")
public class StructureController {

    private final StructureService structureService;

    @PostMapping("/{structure}")
    public Page<Map<String, Object>> getDataStructure(
            @PathVariable String structure,
            @RequestParam(value="object", required = false) String object,
            @RequestBody(required = false) FilterRequest request,
            Pageable pageable,
            Authentication authentication
    ) {
        return structureService.getDataStructure(structure, object, request, pageable);
    }
}
