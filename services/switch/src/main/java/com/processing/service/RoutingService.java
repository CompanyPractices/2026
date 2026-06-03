package com.processing.service;


import org.springframework.stereotype.Service;
import java.util.Map;


@Service
public class RoutingService {


    private static final Map<String, String> BIN_TABLE = Map.of(
            "400000", "ISS001",
            "400001", "ISS002",
            "400002", "ISS003",
            "400003", "ISS004",
            "400004", "ISS005"
    );


    public String getIssuerIdByPan(String pan) {
        if (pan == null || pan.length() < 6) {
            return null;
        }
        String bin = pan.substring(0, 6);
        return BIN_TABLE.get(bin);
    }
}
