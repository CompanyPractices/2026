package com.processing.services;

public interface PanGenerator {
    boolean isValid(String pan);
    String generatePan(String bin);
}
