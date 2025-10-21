package com.glt.connector.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class EncryptionUtils {

    @Value("${security.encryption.key}")
    private String secretKey;
    private SecretKeySpec keySpec;
    private static final String ALGORITHM = "AES";

    @PostConstruct
    public void init() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 16) {
            throw new IllegalArgumentException("AES ключ должен быть 16 байт (128 бит)");
        }

        keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            log.error("Ошибка при шифровании", ex);
            throw new RuntimeException("Encryption failed", ex);
        }
    }

    public String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            log.error("Ошибка при расшифровке", ex);
            throw new RuntimeException("Decryption failed", ex);
        }
    }

}
