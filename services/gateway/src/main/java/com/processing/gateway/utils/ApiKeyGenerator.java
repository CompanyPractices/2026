package com.processing.gateway.utils;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;

@Component
public class ApiKeyGenerator {
    @Value("${gateway.api-keys.length:128}")
    private int keyLength;

    @Value("${gateway.api-keys.algorithm:AES}")
    private String algorithm;

    public String generate() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(algorithm);
            keyGen.init(keyLength);
            SecretKey secretKey = keyGen.generateKey();

            return Base64.toBase64String(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
