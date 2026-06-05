package com.processing.cardmanagement.services;

import com.processing.cardmanagement.annotations.Bin;
import com.processing.cardmanagement.annotations.Pan;

import java.util.Random;

public class LuhnValidator implements PanGenerator {

    private final Random random = new Random();

    @Override
    public boolean isValid(@Pan String pan) {
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

    @Override
    public String generatePan(@Bin String bin) {
        int randomNumber = random.nextInt(100_000_000, 1_000_000_000);
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
