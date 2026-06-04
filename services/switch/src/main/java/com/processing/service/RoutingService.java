package com.processing.service;

import com.processing.config.SwitchProperties;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class RoutingService {

    private final Map<String, String> binTable;

    public RoutingService(SwitchProperties switchProperties) {
        this.binTable = Map.copyOf(switchProperties.binRouting());
    }

    public String getIssuerIdByPan(String pan) {
        if (pan == null || pan.length() < 6) {
            return null;
        }
        String bin = pan.substring(0, 6);
        return binTable.get(bin);
    }
}