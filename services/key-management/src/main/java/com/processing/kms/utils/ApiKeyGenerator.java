package com.processing.kms.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
public class ApiKeyGenerator {
    @Value("${api-keys.length:256}")
    private int keyLength;

    @Value("${api-keys.algorithm:AES}")
    private String algorithm;

    public String generate() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keyLength);
            SecretKey secretKey = keyGen.generateKey();

            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
