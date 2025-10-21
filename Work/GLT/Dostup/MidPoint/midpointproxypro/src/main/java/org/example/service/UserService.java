package org.example.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public interface UserService {

    /**
     * Получение шаблона пользовательских полей с учетом сортировки.
     */
    Map<String, Object> getUserFieldTemplates(String sortParam, String archetype);

    /**
     * Получение пользовательских данных по oid с возможной сортировкой.
     */
    Map<String, Object> getUserFields(UUID oid, String sortParam, String archetype);

    /**
     * Загрузка и возможное сжатие фотографии пользователя.
     */
    ResponseEntity<Object> postPhoto(MultipartFile file, UUID oid) throws IOException;

    /**
     * Получение фотографии пользователя по oid.
     */
    byte[] getPhoto(UUID oid);

    /**
     * Удаление фотографии пользователя по oid.
     */
    ResponseEntity<Object> deletePhoto(UUID oid);
}
