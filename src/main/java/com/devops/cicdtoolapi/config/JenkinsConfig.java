package com.devops.cicdtoolapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jenkins")
public class JenkinsConfig {

    private String url;
    private String user;
    private String apiToken;
    private String pipelineTemplatePath = "jenkins-pipeline-template.xml"; // Default template path

    // --- Getters and Setters ---

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        // Remove trailing slash if present for consistency
        this.url = (url != null && url.endsWith("/")) ? url.substring(0, url.length() - 1) : url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

     public String getPipelineTemplatePath() {
        return pipelineTemplatePath;
    }

    public void setPipelineTemplatePath(String pipelineTemplatePath) {
        this.pipelineTemplatePath = pipelineTemplatePath;
    }
}