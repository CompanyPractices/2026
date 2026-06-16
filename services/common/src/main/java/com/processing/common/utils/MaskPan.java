package com.processing.common.utils;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MaskPan {
    public String maskPan(String pan) {
        if (pan == null) {
            log.warn("null PAN provided for masking");
            return "";
        }
        if (pan.length() != 16) {
            log.warn("invalid PAN length: {}", pan.length());
            return "*".repeat(pan.length());
        }

        return pan.substring(0, 4) + "*".repeat(8) + pan.substring(12);
    }
}
