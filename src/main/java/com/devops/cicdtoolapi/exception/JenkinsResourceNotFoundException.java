package com.devops.cicdtoolapi.exception;

// Specific exception for resource not found (Job/Build)
public class JenkinsResourceNotFoundException extends JenkinsServiceException {
    public JenkinsResourceNotFoundException(String message) {
        super(message);
    }

    public JenkinsResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
