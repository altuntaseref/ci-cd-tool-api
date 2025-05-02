package com.devops.cicdtoolapi.model; // Kendi paketin

import java.util.Map;

public class HealthCheckResult {
    private final String status; // UP, DOWN, UNKNOWN, ERROR
    private final String message;
    private final Map<String, Object> details; // Actuator'dan gelen detaylar (opsiyonel)

    public HealthCheckResult(String status, String message, Map<String, Object> details) {
        this.status = status;
        this.message = message;
        this.details = details;
    }

    // Getters
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Map<String, Object> getDetails() { return details; }
}