package org.example.security.cors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@Component
public class CorsOriginLoader {

    @Value("${cors.origins-file-path}")
    private String originsFilePath;

    public List<String> loadOrigins() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(originsFilePath)) {
            props.load(fis);
            String raw = props.getProperty("allowed-origins");
            if (raw == null || raw.isBlank()) {
                throw new IllegalStateException("Свойство 'allowed-origins' не найдено в файле: " + originsFilePath);
            }
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения CORS-файла: " + originsFilePath, e);
        }
    }
}
