package org.example.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@OpenAPIDefinition(info = @Info(title = "User API", version = "v1"))
@SecurityRequirement(name = "basicAuth")
@Tag(name = "UserController", description = "Взаимодействие с пользователями")
public class UserController {

    private final UserService userService;

    @GetMapping("/getUserInfo")
    public ResponseEntity<Map<String, Object>> getUserFieldTemplates(
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return ResponseEntity.ok(userService.getUserFieldTemplates(sort, "default"));
    }

    @GetMapping("/getUserInfo/archetype/{archetype}") // Для archetype
    public ResponseEntity<Map<String, Object>> getUserFieldTemplatesWithArchetype(
            @PathVariable String archetype,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return ResponseEntity.ok(userService.getUserFieldTemplates(sort, archetype));
    }

    @GetMapping("/getUserInfo/{oid}")
    public ResponseEntity<Map<String, Object>> getUserFields(
            @PathVariable UUID oid,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return ResponseEntity.ok(userService.getUserFields(oid, sort, "default"));
    }

    @GetMapping("/getUserInfo/{oid}/archetype/{archetype}")
    public ResponseEntity<Map<String, Object>> getUserFieldsWithArchetypes(
            @PathVariable UUID oid,
            @PathVariable String archetype,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return ResponseEntity.ok(userService.getUserFields(oid, sort, archetype));
    }


    @Operation(summary = "Загрузка фото пользователя по OID")
    @PutMapping(value = "/user/photo/{oid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE )
    public ResponseEntity<Object> compressImage(@RequestBody MultipartFile file, @PathVariable UUID oid) throws IOException {
        String fileName = file.getOriginalFilename();
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Запрос не содержит обрабатываемый файл");
        }
        if(!fileName.matches(".*\\.(jpg|jpeg|png)$")){
            return ResponseEntity.badRequest().body("Некорректный формат обрабатываемого файла");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON))
                .body(userService.postPhoto(file, oid));
    }

    @Operation(summary = "Получение фото пользователя по OID")
    @GetMapping("/user/photo/{oid}")
    public ResponseEntity<Object> getPhoto(@PathVariable UUID oid) throws IOException{

        byte[] imageByte = userService.getPhoto(oid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .body(imageByte);
    }

    @Operation(summary = "Удаление фото пользователя по OID")
    @DeleteMapping("/user/photo/{oid}")
    public ResponseEntity<Object> deletePhoto(@PathVariable UUID oid){
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.APPLICATION_JSON))
                .body(userService.deletePhoto(oid));
    }

    @Operation(summary = "Получение пользователя по OID")
    @DeleteMapping("/user/self")
    public ResponseEntity<Object> getUserObject(
            Pageable pageable,
            Authentication authentication){
        Object auth = authentication.getCredentials();
        System.out.println(auth);
        auth = authentication.getAuthorities();
        System.out.println();
        return null;
    }
}
