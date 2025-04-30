package com.devops.cicdtoolapi.model;

// Immutable DTO for trigger response
public final class TriggerResult {
    private final String message;
    private final String queueUrl;

    public TriggerResult(String message, String queueUrl) {
        this.message = message;
        this.queueUrl = queueUrl;
    }

    public String getMessage() { return message; }
    public String getQueueUrl() { return queueUrl; }
}