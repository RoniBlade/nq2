package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.example.model.filter.FilterRequest;
import org.example.service.schema.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/schema")
@OpenAPIDefinition(info = @Info(title = "Schema API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
public class SchemaController {

    private final SchemaService schemaService;

    @Autowired
    public SchemaController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    @PostMapping("/{objectType}")
    public ResponseEntity<Map<String, String>> getSchemaFiltered(
            @PathVariable String objectType,
            @RequestBody(required = false) FilterRequest request
    ) {
        Map<String, String> schema = schemaService.getSchemaFor(objectType, request);
        if (schema.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schema);
    }
}
