package org.example.v1.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.example.v1.repository.V1UserRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class V1UserService {

    private final V1UserRepository repository;

    // Если нужно строго 20 KB — поставь 20 * 1024
    private static final int REQUIRED_FILE_SIZE = 160 * 1024;

    public void saveUserPhoto(MultipartFile file, UUID oid) throws IOException { // saveUserPhoto (Сохранить фото)
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Файл пустой");
        String name = (file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
                .toLowerCase(Locale.ROOT);
        if (!name.matches(".*\\.(jpg|jpeg|png)$")) {
            throw new IllegalArgumentException("Допустимы только JPG/PNG");
        }

        byte[] src = file.getBytes();
        byte[] toStore = src.length > REQUIRED_FILE_SIZE ? compress(src) : src;

        // ПЕРЕДАЁМ СТРОКОЙ: кодируем в base64, БД декодирует decode(...,'base64')
        String b64 = Base64.getEncoder().encodeToString(toStore);

        int updated = repository.updatePhotoFromBase64(b64, oid.toString());
        if (updated == 0) throw new IllegalStateException("Пользователь не найден: " + oid);
    }

    public byte[] getUserPhoto(UUID oid) { // getUserPhoto (Получить фото)
        return repository.findPhoto(oid);
    }

    public boolean deleteUserPhoto(UUID oid) { // deleteUserPhoto (Удалить фото)
        return repository.deletePhoto(oid) > 0;
    }

    private byte[] compress(byte[] src) throws IOException { // compress (Сжать изображение)
        boolean isPng = src.length >= 8
                && (src[0] & 0xFF) == 0x89 && src[1] == 'P' && src[2] == 'N' && src[3] == 'G';

        float quality = 0.85f;
        byte[] out = src;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (out.length > REQUIRED_FILE_SIZE && quality >= 0.25f) {
            baos.reset();
            var builder = Thumbnails.of(new ByteArrayInputStream(src)).scale(1.0);
            if (isPng) builder.outputFormat("jpg"); // PNG -> JPEG для эффективного уменьшения
            builder.outputQuality(quality).toOutputStream(baos);
            out = baos.toByteArray();
            quality -= 0.15f;
        }
        return out;
    }

    public static MediaType detectImageType(byte[] bytes) { // detectImageType (Определить тип)
        if (bytes != null && bytes.length >= 4) {
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8) return MediaType.IMAGE_JPEG;
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1]=='P' && bytes[2]=='N' && bytes[3]=='G') return MediaType.IMAGE_PNG;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
