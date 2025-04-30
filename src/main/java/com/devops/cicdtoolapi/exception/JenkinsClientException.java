package com.devops.cicdtoolapi.exception;

// Specific exception for connection/client errors
public class JenkinsClientException extends JenkinsServiceException {
    private final int statusCode; // Optional: store status code for HTTP errors

    public JenkinsClientException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1; // Indicate not an HTTP error or code unknown
    }

    public JenkinsClientException(String message, int statusCode, String responseBody, Throwable cause) {
        super(String.format("%s Status: %d, Response: %s", message, statusCode, responseBody), cause);
        this.statusCode = statusCode;
    }

    public JenkinsClientException(String message, int statusCode) {
        super(String.format("%s Status: %d", message, statusCode));
        this.statusCode = statusCode;
    }


    public int getStatusCode() {
        return statusCode;
    }
}
