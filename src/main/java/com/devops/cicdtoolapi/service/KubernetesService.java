package com.devops.cicdtoolapi.service;

import com.devops.cicdtoolapi.exception.KubernetesAccessException;
import com.devops.cicdtoolapi.exception.KubernetesResourceNotFoundException;
import com.devops.cicdtoolapi.model.AppUrlResult;

public interface KubernetesService {

    /**
     * Retrieves the NodePort access URL for a given application running in Kubernetes.
     * Assumes the Service name follows the pattern "{appName}-service".
     *
     * @param appName   The base name of the application used for naming Kubernetes resources.
     * @param namespace The Kubernetes namespace where the application is deployed.
     * @return AppUrlResult containing the access URL or an error message.
     * @throws KubernetesResourceNotFoundException if the corresponding Service is not found.
     * @throws KubernetesAccessException        if there's an error executing kubectl or parsing the output.
     */
    AppUrlResult getApplicationAccessUrl(String appName, String namespace)
            throws KubernetesResourceNotFoundException, KubernetesAccessException;
}