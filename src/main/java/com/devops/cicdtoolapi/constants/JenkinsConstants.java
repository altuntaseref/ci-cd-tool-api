package com.devops.cicdtoolapi.constants;

public final class JenkinsConstants {

    private JenkinsConstants() { // Prevent instantiation
    }

    // API Paths
    public static final String JOB_PATH = "/job/{jobName}";
    public static final String BUILD_NUMBER_PATH = "/{buildNumber}";
    public static final String LAST_BUILD_PATH = "/lastBuild";
    public static final String API_JSON_SUFFIX = "/api/json";
    public static final String BUILD_TRIGGER_PATH = "/build";
    public static final String CRUMB_ISSUER_PATH = "/crumbIssuer/api/json";
    public static final String CREATE_ITEM_PATH = "/createItem";

    // Headers
    public static final String AUTH_HEADER = "Authorization";
    public static final String CRUMB_HEADER = "Jenkins-Crumb";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";

    // Auth
    public static final String BASIC_AUTH_PREFIX = "Basic ";

    // Status / Results
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_ERROR = "ERROR";
    public static final String STATUS_UNKNOWN = "UNKNOWN";
    public static final String RESULT_SUCCESS = "SUCCESS"; // Example Jenkins result

    // Error Messages (Internal)
    public static final String ERROR_MSG_JOB_NOT_FOUND = "Build or Job not found. Please check job name and build number.";
    public static final String ERROR_MSG_CONNECTION = "Connection Error while communicating with Jenkins: ";
    public static final String ERROR_MSG_HTTP = "HTTP Error communicating with Jenkins: ";
    public static final String ERROR_MSG_CLIENT = "Client Error communicating with Jenkins: ";
    public static final String ERROR_MSG_TEMPLATE_LOAD = "Internal error: Could not load job template.";
    public static final String ERROR_MSG_ENCODING = "Internal error: Could not encode job name.";
     public static final String ERROR_MSG_ALREADY_EXISTS = "Job '%s' already exists.";


}