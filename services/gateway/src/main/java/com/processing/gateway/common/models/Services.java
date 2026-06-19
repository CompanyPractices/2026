package com.processing.gateway.common.models;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Services {
    GATEWAY("gateway"),
    SWITCH("switch"),
    AUTH("authorization"),
    LOGGER("logger"),
    CARDS("cardManagement"),
    TERMINAL("terminalSimulator"),
    MERCHANT("merchantSimulator");

    private final String value;
}
