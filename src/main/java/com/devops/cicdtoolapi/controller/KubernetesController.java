package com.devops.cicdtoolapi.controller;

import com.devops.cicdtoolapi.exception.KubernetesAccessException;
import com.devops.cicdtoolapi.exception.KubernetesResourceNotFoundException;
import com.devops.cicdtoolapi.model.AppUrlResult;
import com.devops.cicdtoolapi.service.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/kubernetes") // API yolu Kubernetes ile ilgili
public class KubernetesController {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesController.class);
    private final KubernetesService kubernetesService;

    public KubernetesController(KubernetesService kubernetesService) {
        this.kubernetesService = kubernetesService;
    }

    /**
     * Retrieves the access URL for a deployed application in Kubernetes.
     * @param appName   The base name of the application.
     * @param namespace The namespace (defaults to 'default').
     * @return ResponseEntity containing the access URL or an error message.
     */
    @GetMapping("/app-url/{appName}")
    public ResponseEntity<?> getApplicationUrl(
            @PathVariable String appName,
            @RequestParam(defaultValue = "default") String namespace
    ) {
        try {
            AppUrlResult result = kubernetesService.getApplicationAccessUrl(appName, namespace);
            // Servis başarıyla URL'yi döndürdü
            return ResponseEntity.ok(result);
        } catch (KubernetesResourceNotFoundException e) {
            logger.warn("Resource not found while getting app URL for '{}': {}", appName, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (KubernetesAccessException e) {
            logger.error("Access exception while getting app URL for '{}': {}", appName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        } catch (Exception e) { // Beklenmedik diğer hatalar
            logger.error("Unexpected error while getting app URL for '{}': {}", appName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred."));
        }
    }
}