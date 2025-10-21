package com.glt.connector.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Encrypter {

    public static void main(String[] args) {
        String secretKey = "MySecretKey12345";
        String plainPassword = "f2jw2QRy2mgA@N";

        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(encrypted);

            System.out.println("Зашифровано: " + base64);
        } catch (Exception e) {
            System.err.println("Ошибка шифрования: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
