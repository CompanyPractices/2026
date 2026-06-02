package com.processing.services;

import java.util.Random;

public class LuhnValidator {
    private static final Random random = new Random();

    public static boolean isValid(String pan) {
        if (pan == null || !pan.matches("\\d+")) {
            throw new IllegalArgumentException("PAN must contain only digits");
        }

        int total = 0;
        boolean isEvenIdx = false;

        for (int i = pan.length() - 1; i >= 0; i--) {
            int digit = pan.charAt(i) - '0';

            if (isEvenIdx) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            total += digit;
            isEvenIdx = !isEvenIdx;
        }

        return total % 10 == 0;
    }

    public static String generatePan(String bin) {
        if (bin == null || !bin.matches("\\d{6}")) {
            throw new IllegalArgumentException("BIN must be exactly 6 digits");
        }

        long randomNumber = random.nextLong(100_000_000L, 1_000_000_000L);
        String base = bin + randomNumber;

        for (int checkDigit = 0; checkDigit < 10; checkDigit++) {
            String pan = base + checkDigit;

            if (isValid(pan)) {
                return pan;
            }
        }

        throw new IllegalStateException("Failed to generate valid PAN for bin: " + bin);
    }
}
