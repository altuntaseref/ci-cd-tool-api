package com.devops.cicdtoolapi.exception;

public class JenkinsServiceException extends RuntimeException {
    public JenkinsServiceException(String message) {
        super(message);
    }

    public JenkinsServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}


