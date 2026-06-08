package com.processing.service;

import com.processing.config.SwitchProperties;
import com.processing.exception.UnknownBinException;
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
            throw new UnknownBinException("null-or-short");
        }
        String bin = pan.substring(0, 6);
        String issuerId = binTable.get(bin);
        if (issuerId == null) {
            throw new UnknownBinException(bin);
        }
        return issuerId;
    }
}