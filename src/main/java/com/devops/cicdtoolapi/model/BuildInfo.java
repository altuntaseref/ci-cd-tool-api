package com.devops.cicdtoolapi.model;

// Immutable DTO for build status response
public final class BuildInfo {
    private final Integer buildNumber;
    private final String buildUrl;
    private final String status; // e.g., RUNNING, SUCCESS, FAILURE, ERROR
    private final String result; // Raw result from Jenkins or specific error message

    public BuildInfo(Integer buildNumber, String buildUrl, String status, String result) {
        this.buildNumber = buildNumber;
        this.buildUrl = buildUrl;
        this.status = status;
        this.result = result;
    }

    public Integer getBuildNumber() { return buildNumber; }
    public String getBuildUrl() { return buildUrl; }
    public String getStatus() { return status; }
    public String getResult() { return result; } // Can contain Jenkins result or error details
}