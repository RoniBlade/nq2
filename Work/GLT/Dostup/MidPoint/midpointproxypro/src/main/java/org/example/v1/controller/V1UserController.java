package org.example.v1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.v1.service.V1UserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/api/user")
@Tag(name = "UserController", description = "Получение, создание и удаление фотографий пользователя")
public class V1UserController {

    private final V1UserService userService;

    @Operation(summary = "Загрузка фото пользователя по OID")
    @PutMapping(value = "/photo/{oid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> uploadUserPhoto( // uploadUserPhoto (Загрузка фото пользователя)
                                              @PathVariable UUID oid,
                                              @RequestPart("file") MultipartFile file
    ) throws IOException {
        userService.saveUserPhoto(file, oid);
        return ResponseEntity.accepted().body(Map.of("message", "Фотография загружена"));
    }

    @Operation(summary = "Получение фото пользователя по OID")
    @GetMapping("/photo/{oid}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable UUID oid) {
        byte[] photo = userService.getUserPhoto(oid);
        if (photo == null) return ResponseEntity.ok().body(null);
        return ResponseEntity.ok()
                .contentType(V1UserService.detectImageType(photo))
                .body(photo);
    }

    @Operation(summary = "Удаление фото пользователя по OID")
    @DeleteMapping(value = "/photo/{oid}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deletePhoto(@PathVariable UUID oid) {
        return ResponseEntity.ok(Map.of("deleted", userService.deleteUserPhoto(oid)));
    }
}
