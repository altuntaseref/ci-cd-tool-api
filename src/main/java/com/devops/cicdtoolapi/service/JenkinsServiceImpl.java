package com.devops.cicdtoolapi.service;

import com.devops.cicdtoolapi.client.JenkinsHttpClient;
import com.devops.cicdtoolapi.config.JenkinsConfig;
import com.devops.cicdtoolapi.model.*;
import com.devops.cicdtoolapi.exception.JenkinsClientException;
import com.devops.cicdtoolapi.exception.JenkinsJobAlreadyExistsException;
import com.devops.cicdtoolapi.exception.JenkinsResourceNotFoundException;
import com.devops.cicdtoolapi.exception.JenkinsServiceException;
import com.devops.cicdtoolapi.model.BuildInfo;
import com.devops.cicdtoolapi.constants.JenkinsConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class JenkinsServiceImpl implements JenkinsService {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsServiceImpl.class);

    private final JenkinsHttpClient jenkinsHttpClient;
    private final JenkinsConfig jenkinsConfig;


    // Internal DTO for mapping Jenkins API response for build status


    public JenkinsServiceImpl(JenkinsHttpClient jenkinsHttpClient, JenkinsConfig jenkinsConfig) {
        this.jenkinsHttpClient = jenkinsHttpClient;
        this.jenkinsConfig = jenkinsConfig;
    }

    @Override
    public BuildInfo getJobBuildStatus(String jobName, Integer buildNumber) {
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("jobName", jobName);

        String buildPathSegment;
        String buildIdentifier; // For logging

        if (buildNumber != null) {
            uriVariables.put("buildNumber", buildNumber);
            buildPathSegment = JenkinsConstants.BUILD_NUMBER_PATH;
            buildIdentifier = buildNumber.toString();
        } else {
            buildPathSegment = JenkinsConstants.LAST_BUILD_PATH;
            buildIdentifier = "lastBuild";
        }

        String apiPath = JenkinsConstants.JOB_PATH + buildPathSegment + JenkinsConstants.API_JSON_SUFFIX;

        logger.info("Fetching build status for job '{}' (Build: {})", jobName, buildIdentifier);

        try {
            ResponseEntity<JenkinsBuildApiResponse> response = jenkinsHttpClient.get(
                    apiPath, uriVariables, JenkinsBuildApiResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JenkinsBuildApiResponse apiResponse = response.getBody();
                BuildInfo buildInfo = mapApiResponseToBuildInfo(apiResponse);
                logger.info("Status for job '{}', build {}: {}", jobName, buildInfo.getBuildNumber(), buildInfo.getStatus());
                return buildInfo;
            } else {
                // Should generally be caught by HttpClientErrorException, but as fallback
                logger.warn("Received unexpected status {} for job '{}' status check.", response.getStatusCode(), jobName);
                throw new JenkinsServiceException("Failed to get build status. Unexpected response: " + response.getStatusCode());
            }
        } catch (JenkinsClientException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                 logger.warn("Job '{}' or build '{}' not found.", jobName, buildIdentifier);
                 throw new JenkinsResourceNotFoundException(JenkinsConstants.ERROR_MSG_JOB_NOT_FOUND, e);
            }
             logger.error("Error getting build status for job '{}': {}", jobName, e.getMessage(), e);
             throw new JenkinsServiceException("Failed to retrieve build status for job '" + jobName + "'. Reason: " + e.getMessage(), e);
        }
    }


    @Override
    public TriggerResult triggerSimpleBuild(String jobName) {
         String apiPath = JenkinsConstants.JOB_PATH + JenkinsConstants.BUILD_TRIGGER_PATH;
         Map<String, Object> uriVariables = Collections.singletonMap("jobName", jobName);

        logger.info("Triggering Jenkins job '{}'", jobName);

        try {
             // POST with empty body (Void.class), expect String response (though we mainly care about status/headers)
             ResponseEntity<String> response = jenkinsHttpClient.post(
                     apiPath, uriVariables, null, String.class, null); // No specific content type needed for simple trigger

             // Jenkins typically returns 201 Created on successful trigger
             if (response.getStatusCode() == HttpStatus.CREATED) {
                 URI queueLocation = response.getHeaders().getLocation();
                 String queueUrl = (queueLocation != null) ? queueLocation.toString() : "N/A";
                 String message = String.format("Job '%s' triggered successfully.", jobName);
                 logger.info("Jenkins job '{}' triggered successfully. Queue URL: {}", jobName, queueUrl);
                 return new TriggerResult(message, queueUrl);
             } else {
                 // Should not happen often as errors usually throw exceptions
                 logger.error("Failed to trigger Jenkins job '{}'. Unexpected Status: {}", jobName, response.getStatusCode());
                 throw new JenkinsServiceException("Failed to trigger job '" + jobName + "'. Unexpected Status: " + response.getStatusCode());
             }
         } catch (JenkinsClientException e) {
             if (e.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                 logger.warn("Job '{}' not found for triggering.", jobName);
                 throw new JenkinsResourceNotFoundException("Cannot trigger job: Job '" + jobName + "' not found.", e);
             }
             // Handle other HTTP errors (401, 403, 5xx)
             logger.error("Error triggering Jenkins job '{}': {}", jobName, e.getMessage(), e);
             throw new JenkinsServiceException("Failed to trigger job '" + jobName + "'. Reason: " + e.getMessage(), e);
         }
    }

    @Override
    public CreateJobResult createPipelineJob(JenkinsJobRequest request) {
        String jobName = request.getJobName();
        logger.info("Attempting to create Jenkins pipeline job '{}'", jobName);

        // 1. Load and prepare XML config
        String jobConfigXml = prepareJobConfigXml(jobName);

        // 2. Prepare API request
        String encodedJobName;
        try {
            encodedJobName = URLEncoder.encode(jobName, StandardCharsets.UTF_8.toString());
        } catch (Exception e) {
            logger.error("Could not encode job name: {}", jobName, e);
            throw new JenkinsServiceException(JenkinsConstants.ERROR_MSG_ENCODING, e);
        }

        String apiPath = JenkinsConstants.CREATE_ITEM_PATH + "?name=" + encodedJobName; // Use query param for createItem

        // 3. Execute API call
        try {
             // POST with XML body, expect String response (Jenkins often just returns HTML/empty on success)
            ResponseEntity<String> response = jenkinsHttpClient.post(
                    apiPath, Collections.emptyMap(), // No path variables here
                    jobConfigXml, String.class, MediaType.APPLICATION_XML);

            // Jenkins usually returns 200 OK for successful creation via this endpoint
            if (response.getStatusCode() == HttpStatus.OK) {
                String message = String.format("Job '%s' created successfully.", jobName);
                logger.info(message);
                return new CreateJobResult(message);
            } else {
                 // Should not happen often
                logger.error("Failed to create Jenkins job '{}'. Unexpected Status: {}", jobName, response.getStatusCode());
                throw new JenkinsServiceException("Failed to create job '" + jobName + "'. Unexpected Status: " + response.getStatusCode());
            }
        } catch (JenkinsClientException e) {
             // Check specifically for "already exists" error (often a 400 Bad Request)
             if (e.getStatusCode() == HttpStatus.BAD_REQUEST.value() && e.getMessage() != null && e.getMessage().contains("already exists")) {
                String errorMessage = String.format(JenkinsConstants.ERROR_MSG_ALREADY_EXISTS, jobName);
                logger.warn("Could not create Jenkins job '{}': Job already exists.", jobName);
                throw new JenkinsJobAlreadyExistsException(errorMessage);
             } else {
                 logger.error("Error creating Jenkins job '{}': {}", jobName, e.getMessage(), e);
                 throw new JenkinsServiceException("Failed to create job '" + jobName + "'. Reason: " + e.getMessage(), e);
             }
        }
    }

    // --- Helper Methods ---

     private String prepareJobConfigXml(String jobName) {
        try {
            Resource resource = new ClassPathResource(jenkinsConfig.getPipelineTemplatePath());
            if (!resource.exists()) {
                 logger.error("Jenkins job template not found at path: {}", jenkinsConfig.getPipelineTemplatePath());
                 throw new JenkinsServiceException(JenkinsConstants.ERROR_MSG_TEMPLATE_LOAD + " Template file not found.");
            }
            try (InputStream inputStream = resource.getInputStream()) {
                String xmlConfigTemplate = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
                // Basic placeholder replacement - enhance if more complex templating needed
                return xmlConfigTemplate.replace("${serviceName}", jobName);
            }
        } catch (IOException e) {
            logger.error("Could not load or read Jenkins job template from path: {}", jenkinsConfig.getPipelineTemplatePath(), e);
            throw new JenkinsServiceException(JenkinsConstants.ERROR_MSG_TEMPLATE_LOAD, e);
        }
    }

    /** Maps the raw Jenkins API response to our BuildInfo DTO. */
    private BuildInfo mapApiResponseToBuildInfo(JenkinsBuildApiResponse apiResponse) {
        String status = JenkinsConstants.STATUS_UNKNOWN;
        String result = apiResponse.getResult(); // Keep the original result

        if (apiResponse.isBuilding()) {
            status = JenkinsConstants.STATUS_RUNNING;
        } else if (result != null) {
            // Use the result directly if build is finished (SUCCESS, FAILURE, ABORTED, etc.)
            // We convert to uppercase for consistency, matching Jenkins UI often
             status = result.toUpperCase();
        }
        // If not building and result is null, it might be queued or in a strange state
        // Keep status as UNKNOWN in this edge case

        return new BuildInfo(
                apiResponse.getNumber(),
                apiResponse.getUrl(),
                status,
                result // Return the raw result string from Jenkins
        );
    }
}