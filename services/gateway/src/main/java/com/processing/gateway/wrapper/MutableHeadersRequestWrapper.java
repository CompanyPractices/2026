package com.processing.gateway.wrapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.*;

public class MutableHeadersRequestWrapper extends HttpServletRequestWrapper {
    private final Map<String, String> mutableHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public MutableHeadersRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    public void addHeader(String name, String value) {
        mutableHeaders.put(name, value);
    }

    @Override
    public String getHeader(String name) {
        String value = mutableHeaders.get(name);

        return value != null ? value : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> headerNames = Collections.list(super.getHeaderNames());
        headerNames.addAll(mutableHeaders.keySet());

        return Collections.enumeration(headerNames);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> headers = Collections.list(super.getHeaders(name));

        if (mutableHeaders.containsKey(name))
            headers.add(mutableHeaders.get(name));

        return Collections.enumeration(headers);
    }
}
