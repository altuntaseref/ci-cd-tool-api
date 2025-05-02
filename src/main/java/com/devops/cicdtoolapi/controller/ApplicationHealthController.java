package com.devops.cicdtoolapi.controller; // Kendi paketin

import com.devops.cicdtoolapi.model.AppUrlResult; // URL almak için belki bu da gerekir
import com.devops.cicdtoolapi.model.HealthCheckResult;
import com.devops.cicdtoolapi.service.ApplicationHealthService;
import com.devops.cicdtoolapi.service.KubernetesService; // URL almak için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/app-health") // Yeni API yolu
public class ApplicationHealthController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationHealthController.class);
    private final ApplicationHealthService healthService;
    private final KubernetesService kubernetesService;

    public ApplicationHealthController(ApplicationHealthService healthService, KubernetesService kubernetesService) {
        this.healthService = healthService;
        this.kubernetesService = kubernetesService;
    }

    /**
     * Retrieves the health status of a deployed application by its name.
     * It first gets the application URL via KubernetesService and then checks its health.
     *
     * @param appName   The base name of the application.
     * @param namespace The namespace (defaults to 'default').
     * @return ResponseEntity containing the health status or an error.
     */
    @GetMapping("/check/{appName}")
    public ResponseEntity<?> checkApplicationHealthByName(
            @PathVariable String appName,
            @RequestParam(defaultValue = "default") String namespace
    ) {
        // 1. Get the application URL first
        AppUrlResult urlResult;
        try {
            urlResult = kubernetesService.getApplicationAccessUrl(appName, namespace);
            if (!urlResult.isSuccess() || urlResult.getAccessUrl() == null) {
                logger.warn("Could not get URL for app '{}' to check health. Reason: {}", appName, urlResult.getMessage());
                // KubernetesService'in hatasına göre uygun status dön
                HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
                if(urlResult.getMessage() != null && urlResult.getMessage().contains("not found")) status = HttpStatus.NOT_FOUND;
                return ResponseEntity.status(status).body(Map.of("message", "Could not determine application URL: " + urlResult.getMessage()));
            }
        } catch (Exception e) { // KubernetesService'den gelen exception'ları yakala
             logger.error("Error getting application URL for health check of '{}': {}", appName, e.getMessage(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error retrieving application URL: " + e.getMessage()));
        }


        // 2. Check the health using the retrieved URL
        logger.info("Checking health for application '{}' using URL: {}", appName, urlResult.getAccessUrl());
        HealthCheckResult healthResult = healthService.checkApplicationHealth(urlResult.getAccessUrl());

        // Yanıtı oluştur
        HttpStatus responseStatus;
        switch (healthResult.getStatus().toUpperCase()) {
            case "UP":
                responseStatus = HttpStatus.OK; // 200 OK
                break;
            case "DOWN":
                responseStatus = HttpStatus.SERVICE_UNAVAILABLE; // 503 Service Unavailable
                break;
            case "UNKNOWN":
                 responseStatus = HttpStatus.EXPECTATION_FAILED; // 417 olabilir veya 503
                 break;
            case "ERROR":
            default:
                responseStatus = HttpStatus.INTERNAL_SERVER_ERROR; // 500
                break;
        }

        return ResponseEntity.status(responseStatus).body(healthResult);
    }

}