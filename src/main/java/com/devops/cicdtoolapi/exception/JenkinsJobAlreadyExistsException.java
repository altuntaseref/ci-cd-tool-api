package com.devops.cicdtoolapi.exception;

// Specific exception for Job Already Exists
public class JenkinsJobAlreadyExistsException extends JenkinsServiceException {
    public JenkinsJobAlreadyExistsException(String message) {
        super(message);
    }
}
