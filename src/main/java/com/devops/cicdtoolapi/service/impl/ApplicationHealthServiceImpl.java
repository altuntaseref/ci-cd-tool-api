package com.devops.cicdtoolapi.service.impl; // Kendi paketin

import com.devops.cicdtoolapi.model.HealthCheckResult;
import com.devops.cicdtoolapi.service.ApplicationHealthService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationHealthServiceImpl implements ApplicationHealthService {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationHealthServiceImpl.class);
    private final RestTemplate restTemplate;

    public ApplicationHealthServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public HealthCheckResult checkApplicationHealth(String applicationUrl) {
        if (applicationUrl == null || applicationUrl.isEmpty()) {
            return new HealthCheckResult("ERROR", "Application URL is missing.", Collections.emptyMap());
        }

        // Actuator health endpoint URL'sini oluştur
        String healthUrl = applicationUrl.endsWith("/")
                         ? applicationUrl + "actuator/health"
                         : applicationUrl + "/actuator/health";

        logger.info("Checking application health at URL: {}", healthUrl);

        try {
            // GET isteği gönder, yanıtı ActuatorHealthResponse olarak almayı dene
            ResponseEntity<ActuatorHealthResponse> response = restTemplate.exchange(
                    healthUrl, HttpMethod.GET, null, ActuatorHealthResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Başarılı yanıt (200 OK) genellikle "UP" durumunu gösterir
                ActuatorHealthResponse healthResponse = response.getBody();
                logger.info("Application at {} is {}", applicationUrl, healthResponse.getStatus());
                Map<String, Object> getMapComponents = new HashMap<>();

                getMapComponents.put("URL",applicationUrl);
                return new HealthCheckResult(
                        healthResponse.getStatus() != null ? healthResponse.getStatus().toUpperCase() : "UNKNOWN",
                        "Health check successful.",
                        getMapComponents // Detayları da alalım (yapılandırıldıysa)
                );
            } else if (response.getStatusCode().is5xxServerError()) {
                 // 5xx (örn: 503 Service Unavailable) genellikle "DOWN" durumunu gösterir
                 logger.warn("Application at {} returned status {}. Considered DOWN.", applicationUrl, response.getStatusCode());
                  return new HealthCheckResult("DOWN", "Health endpoint returned status: " + response.getStatusCode(), Collections.emptyMap());
            }
            else {
                // Diğer beklenmedik durum kodları
                logger.warn("Application at {} returned unexpected status {}.", applicationUrl, response.getStatusCode());
                return new HealthCheckResult("UNKNOWN", "Health endpoint returned unexpected status: " + response.getStatusCode(), Collections.emptyMap());
            }
        } catch (HttpClientErrorException e) {
            // 4xx veya 5xx hataları (örn: 404 Not Found - endpoint yok, 503 Service Unavailable)
             logger.warn("HTTP error checking health for {}: {} - {}", applicationUrl, e.getStatusCode(), e.getResponseBodyAsString());
             if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                 return new HealthCheckResult("ERROR", "Health endpoint not found at " + healthUrl, Collections.emptyMap());
             }
             // Diğer 5xx hataları da DOWN olarak yorumlanabilir
             if(e.getStatusCode().is5xxServerError()){
                 return new HealthCheckResult("DOWN", "Health endpoint returned error status: " + e.getStatusCode(), Collections.emptyMap());
             }
             return new HealthCheckResult("UNKNOWN", "HTTP Error: " + e.getStatusCode(), Collections.emptyMap());
        } catch (ResourceAccessException e) {
            // Bağlantı hatası (sunucuya ulaşılamıyor)
             logger.warn("Connection error checking health for {}: {}", applicationUrl, e.getMessage());
             return new HealthCheckResult("DOWN", "Connection refused or timed out.", Collections.emptyMap());
        } catch (RestClientException e) {
            // Diğer RestTemplate hataları
            logger.error("Error checking health for {}: {}", applicationUrl, e.getMessage(), e);
            return new HealthCheckResult("ERROR", "Error during health check: " + e.getMessage(), Collections.emptyMap());
        }
    }

    // Actuator /health yanıtını parse etmek için DTO
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ActuatorHealthResponse {
        private String status;
        private Map<String, Object> components; // Detaylar için (show-details: always/when-authorized)

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Map<String, Object> getComponents() { return components; }
        public void setComponents(Map<String, Object> components) { this.components = components; }
    }
}