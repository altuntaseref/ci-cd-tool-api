package com.devops.cicdtoolapi.service.impl;

import com.devops.cicdtoolapi.exception.KubernetesAccessException;
import com.devops.cicdtoolapi.exception.KubernetesResourceNotFoundException;
import com.devops.cicdtoolapi.model.AppUrlResult; // DTO'yu import et
import com.devops.cicdtoolapi.service.KubernetesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class KubernetesServiceImpl implements KubernetesService {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesServiceImpl.class);

    // Kubeconfig dosyasının yolu
    @Value("${kube.config.path:${user.home}/.kube/config}")
    private String kubeConfigPath;

    // Servis adı için sonek
    private static final String SERVICE_SUFFIX = "-service";
    private static final String NODE_HOST = "localhost"; // Docker Desktop için varsayım

    @Override
    public AppUrlResult getApplicationAccessUrl(String appName, String namespace)
            throws KubernetesResourceNotFoundException, KubernetesAccessException {

        String serviceName = appName + SERVICE_SUFFIX;
        logger.info("Attempting to find NodePort for Kubernetes service '{}' in namespace '{}'", serviceName, namespace);

        // kubectl komutunu oluştur
        String command = String.format(
                "kubectl --kubeconfig=\"%s\" get service %s -n %s -o jsonpath=\"{.spec.ports[0].nodePort}\"",
                kubeConfigPath, serviceName, namespace);

        logger.debug("Executing command: {}", command);

        Process process;
        String nodePortString;
        String errorOutput;
        int exitCode;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
             if (System.getProperty("os.name").toLowerCase().contains("win")) {
                 processBuilder.command("cmd", "/c", command);
             } else {
                 processBuilder.command("/bin/sh", "-c", command);
             }
            process = processBuilder.start();

            // Çıktıları oku
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                nodePortString = reader.readLine(); // Genellikle tek satır beklenir
            }
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                errorOutput = errorReader.lines().collect(java.util.stream.Collectors.joining("\n"));
            }

            // Timeout ile bekle
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                 process.destroyForcibly();
                 logger.error("kubectl command timed out for service '{}'", serviceName);
                 throw new KubernetesAccessException("kubectl command timed out while getting NodePort.");
            }
            exitCode = process.exitValue();

        } catch (IOException e) {
             logger.error("IOException executing kubectl command for service '{}': {}", serviceName, e.getMessage(), e);
             throw new KubernetesAccessException("IOException during kubectl execution: " + e.getMessage(), e);
        } catch (InterruptedException e) {
             logger.error("kubectl command interrupted for service '{}': {}", serviceName, e.getMessage(), e);
             Thread.currentThread().interrupt();
             throw new KubernetesAccessException("kubectl command was interrupted.", e);
        }

        // Sonucu işle
        if (exitCode == 0 && nodePortString != null && !nodePortString.isEmpty()) {
            nodePortString = nodePortString.trim();
            try {
                Integer.parseInt(nodePortString); // Sayı mı diye kontrol et
                String accessUrl = "http://" + NODE_HOST + ":" + nodePortString;
                logger.info("Successfully retrieved NodePort {} for service '{}'. Access URL: {}", nodePortString, serviceName, accessUrl);
                return new AppUrlResult(true, accessUrl, "Successfully retrieved access URL.");
            } catch (NumberFormatException nfe) {
                 logger.error("Command output for NodePort was not a valid number: '{}'. Full output: {}", nodePortString, nodePortString);
                 throw new KubernetesAccessException("Could not parse NodePort from kubectl output: " + nodePortString);
            }
        } else {
            logger.error("kubectl command failed with exit code {} or returned empty NodePort. Service: '{}'. Error output: {}", exitCode, serviceName, errorOutput);
            if (errorOutput != null && errorOutput.toLowerCase().contains("not found")) {
                throw new KubernetesResourceNotFoundException("Service '" + serviceName + "' not found in namespace '" + namespace + "'.");
            }
            throw new KubernetesAccessException("Failed to execute kubectl command or find NodePort. Exit code: " + exitCode + ". Error: " + errorOutput);
        }
    }
}