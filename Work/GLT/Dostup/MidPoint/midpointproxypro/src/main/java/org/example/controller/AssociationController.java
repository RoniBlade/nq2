package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.GroupDto;
import org.example.service.view.AssociationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/groups")
@OpenAPIDefinition(info = @Info(title = "Groups API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "AssociationController ", description = "Взаимодействие с информацией о записях из таблицы shadow")
public class AssociationController {

    private final AssociationService AssociationService;

    public AssociationController(AssociationService associationService) {
        this.AssociationService = associationService;
    }

    /**
     * Получить список групп (с displayName) по oid пользователя
     *
     * Пример запроса:
     * GET /api/groups/1fcb7c34-8d39-4cc4-bf89-4d0a78d41abc
     */
    @GetMapping("/{oid}")
    public ResponseEntity<List<GroupDto>> getGroups(@PathVariable UUID oid) {
        return ResponseEntity.ok(AssociationService.getGroupsByOid(oid));
    }

}
