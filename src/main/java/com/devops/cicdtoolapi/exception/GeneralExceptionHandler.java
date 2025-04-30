package com.devops.cicdtoolapi.exception;

import com.devops.cicdtoolapi.controller.JenkinsTriggerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

public class GeneralExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsTriggerController.class);

    @ExceptionHandler(JenkinsResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(JenkinsResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(JenkinsJobAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleJobExists(JenkinsJobAlreadyExistsException ex) {
        logger.warn("Job creation conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT) // 409 Conflict is suitable here
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(JenkinsServiceException.class)
    public ResponseEntity<Map<String, String>> handleServiceException(JenkinsServiceException ex) {
        logger.error("Jenkins service error: {}", ex.getMessage(), ex);
        // Default to 500, but could inspect the cause if needed for more specific statuses
        // e.g., check if cause is JenkinsClientException and look at statusCode
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getCause() instanceof JenkinsClientException) {
            JenkinsClientException cause = (JenkinsClientException) ex.getCause();
            if (cause.getStatusCode() == HttpStatus.UNAUTHORIZED.value()){
                status = HttpStatus.UNAUTHORIZED;
            } else if (cause.getStatusCode() == HttpStatus.FORBIDDEN.value()) {
                status = HttpStatus.FORBIDDEN;
            } else if (cause.getStatusCode() > 0 && cause.getStatusCode() >= 500) {
                // Treat underlying 5xx errors from Jenkins as Service Unavailable
                status = HttpStatus.SERVICE_UNAVAILABLE;
            }
            // Add more mappings if needed based on status codes
        }

        return ResponseEntity.status(status)
                .body(Map.of("message", "An error occurred interacting with Jenkins: " + ex.getMessage()));
    }

    // Optional: A general handler for unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        logger.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "An internal server error occurred."));
    }
}
