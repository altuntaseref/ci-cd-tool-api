package com.devops.cicdtoolapi.service; // Kendi paketin

import com.devops.cicdtoolapi.model.HealthCheckResult; // Yeni DTO

public interface ApplicationHealthService {

    /**
     * Checks the health of a deployed application using its Actuator endpoint.
     * @param applicationUrl The base URL of the deployed application (e.g., "http://localhost:31500").
     * @return HealthCheckResult containing the status (UP, DOWN, UNKNOWN) and details.
     */
    HealthCheckResult checkApplicationHealth(String applicationUrl);
}