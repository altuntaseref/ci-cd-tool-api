package com.devops.cicdtoolapi.exception;

public class KubernetesResourceNotFoundException extends RuntimeException {
    public KubernetesResourceNotFoundException(String message) { super(message); }
    public KubernetesResourceNotFoundException(String message, Throwable cause) { super(message, cause); }
}