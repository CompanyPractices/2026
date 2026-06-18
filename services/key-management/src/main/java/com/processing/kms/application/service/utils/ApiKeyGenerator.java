package com.processing.kms.application.service.utils;

import com.processing.kms.application.service.properties.KmsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class ApiKeyGenerator {
    private final KmsProperties kmsProperties;

    public String generate() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(kmsProperties.getGeneration().getAlgorithm());
            keyGen.init(kmsProperties.getGeneration().getLength());
            SecretKey secretKey = keyGen.generateKey();

            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
