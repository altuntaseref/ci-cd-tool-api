package com.devops.cicdtoolapi.model;

public class AppUrlResult {
    private final boolean success;
    private final String accessUrl;
    private final String message;
    // Constructor ve Getters...
    public AppUrlResult(boolean success, String accessUrl, String message) { this.success=success; this.accessUrl=accessUrl; this.message=message; }
    public boolean isSuccess() { return success; }
    public String getAccessUrl() { return accessUrl; }
    public String getMessage() { return message; }
}