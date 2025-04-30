package com.devops.cicdtoolapi.model;

// Immutable DTO for job creation response
public final class CreateJobResult {
     private final String message;

    public CreateJobResult(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}