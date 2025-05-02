package com.devops.cicdtoolapi.exception;

public class KubernetesAccessException extends RuntimeException {
    public KubernetesAccessException(String message) { super(message); }
    public KubernetesAccessException(String message, Throwable cause) { super(message, cause); }
}