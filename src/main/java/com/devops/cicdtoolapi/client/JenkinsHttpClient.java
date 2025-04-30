package com.devops.cicdtoolapi.client;

import com.devops.cicdtoolapi.config.JenkinsConfig;
import com.devops.cicdtoolapi.constants.JenkinsConstants;
import com.devops.cicdtoolapi.exception.JenkinsClientException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * Handles low-level HTTP communication with the Jenkins API.
 * Manages authentication, CSRF tokens, and basic request execution.
 */
@Component // Or @Service
public class JenkinsHttpClient {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsHttpClient.class);

    private final RestTemplate restTemplate;
    private final JenkinsConfig jenkinsConfig;
    private String cachedCrumb = null; // Simple cache for crumb

    public JenkinsHttpClient(RestTemplate restTemplate, JenkinsConfig jenkinsConfig) {
        this.restTemplate = restTemplate;
        this.jenkinsConfig = jenkinsConfig;
    }

    /**
     * Executes a GET request.
     * @param path API path (relative to Jenkins base URL)
     * @param uriVariables Path variables for the URL template
     * @param responseType Expected response type
     * @param <T> Response type
     * @return ResponseEntity containing the response
     * @throws JenkinsClientException on communication errors
     */
    public <T> ResponseEntity<T> get(String path, Map<String, ?> uriVariables, Class<T> responseType) {
        String url = buildUrl(path);
        HttpEntity<Void> entity = new HttpEntity<>(createBasicAuthHeaders());
        logger.debug("Executing GET request to: {} with variables: {}", url, uriVariables);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, responseType, uriVariables);
        } catch (HttpClientErrorException e) {
            logger.error("HTTP error during GET to {}: {} - {}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
             throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_HTTP + e.getMessage(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            logger.error("Client error during GET to {}: {}", url, e.getMessage(), e);
            throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_CLIENT + e.getMessage(), e);
        }
    }

    /**
     * Executes a POST request with an optional body.
     * Handles CSRF crumb retrieval and inclusion.
     * @param path API path (relative to Jenkins base URL)
     * @param uriVariables Path variables for the URL template
     * @param requestBody Optional request body
     * @param responseType Expected response type
     * @param contentType Content-Type header for the request
     * @param <T> Response type
     * @param <B> Request body type
     * @return ResponseEntity containing the response
     * @throws JenkinsClientException on communication errors
     */
    public <T, B> ResponseEntity<T> post(String path, Map<String, ?> uriVariables, B requestBody, Class<T> responseType, MediaType contentType) {
        String url = buildUrl(path);
        HttpHeaders headers = createAuthenticatedHeadersWithCrumb();
        if (contentType != null) {
            headers.setContentType(contentType);
        }

        HttpEntity<B> entity = new HttpEntity<>(requestBody, headers);
        logger.debug("Executing POST request to: {} with variables: {}", url, uriVariables);
        try {
            return restTemplate.exchange(url, HttpMethod.POST, entity, responseType, uriVariables);
        } catch (HttpClientErrorException e) {
             // Check for 403 Forbidden specifically for potential crumb issues
             if (e.getStatusCode() == HttpStatus.FORBIDDEN && cachedCrumb != null) {
                  logger.warn("Received 403 Forbidden, potentially stale crumb. Invalidating crumb and retrying once.");
                  invalidateCrumb(); // Invalidate cache
                  headers = createAuthenticatedHeadersWithCrumb(); // Get a fresh crumb
                  entity = new HttpEntity<>(requestBody, headers); // Recreate entity
                  try {
                      return restTemplate.exchange(url, HttpMethod.POST, entity, responseType, uriVariables);
                  } catch (HttpClientErrorException nestedE) {
                      logger.error("HTTP error during POST retry to {}: {} - {}", url, nestedE.getStatusCode(), nestedE.getResponseBodyAsString(), nestedE);
                      throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_HTTP + nestedE.getMessage(), nestedE.getStatusCode().value(), nestedE.getResponseBodyAsString(), nestedE);
                  } catch (RestClientException nestedE) {
                      logger.error("Client error during POST retry to {}: {}", url, nestedE.getMessage(), nestedE);
                      throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_CLIENT + nestedE.getMessage(), nestedE);
                  }
             } else {
                 logger.error("HTTP error during POST to {}: {} - {}", url, e.getStatusCode(), e.getResponseBodyAsString(), e);
                 throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_HTTP + e.getMessage(), e.getStatusCode().value(), e.getResponseBodyAsString(), e);
             }
        } catch (RestClientException e) {
            logger.error("Client error during POST to {}: {}", url, e.getMessage(), e);
            throw new JenkinsClientException(JenkinsConstants.ERROR_MSG_CLIENT + e.getMessage(), e);
        }
    }


    private String buildUrl(String path) {
        return jenkinsConfig.getUrl() + path;
    }


    private HttpHeaders createBasicAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = jenkinsConfig.getUser() + ":" + jenkinsConfig.getApiToken();
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = JenkinsConstants.BASIC_AUTH_PREFIX + new String(encodedAuth);
        headers.set(JenkinsConstants.AUTH_HEADER, authHeader);
        return headers;
    }

    private HttpHeaders createAuthenticatedHeadersWithCrumb() {
        HttpHeaders headers = createBasicAuthHeaders();
        getCrumb().ifPresent(crumb -> headers.set(JenkinsConstants.CRUMB_HEADER, crumb));
        return headers;
    }

     // Invalidate the cached crumb
    private void invalidateCrumb() {
        logger.debug("Invalidating cached Jenkins crumb.");
        this.cachedCrumb = null;
    }


    // Optional wrapper around crumb retrieval with caching
    private Optional<String> getCrumb() {
        if (cachedCrumb != null) {
            logger.debug("Using cached Jenkins crumb.");
            return Optional.of(cachedCrumb);
        }

        String crumbUrl = buildUrl(JenkinsConstants.CRUMB_ISSUER_PATH);
        HttpHeaders headers = createBasicAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            // Use Map.class for flexibility, Jenkins might change the response slightly
            ResponseEntity<Map> response = restTemplate.exchange(crumbUrl, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String crumbValue = (String) response.getBody().get("crumb");
                if (crumbValue != null) {
                    logger.info("Successfully retrieved Jenkins crumb.");
                    this.cachedCrumb = crumbValue; // Cache the crumb
                    return Optional.of(crumbValue);
                } else {
                     logger.warn("Crumb field not found in response from {}", crumbUrl);
                     return Optional.empty();
                }
            } else {
                logger.warn("Could not retrieve Jenkins crumb from {}, status: {}", crumbUrl, response.getStatusCode());
                return Optional.empty(); // CSRF protection might be off or other issue
            }
        } catch (HttpClientErrorException.NotFound notFound) {
             logger.warn("Crumb issuer endpoint not found at {}. CSRF protection might be disabled.", crumbUrl);
             return Optional.empty();
        } catch (RestClientException e) {
            // Log error but don't throw exception, allow requests to proceed without crumb
            logger.error("Error retrieving Jenkins crumb from {}: {}", crumbUrl, e.getMessage());
            return Optional.empty();
        }
    }
}