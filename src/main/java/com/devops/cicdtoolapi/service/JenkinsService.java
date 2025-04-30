package com.devops.cicdtoolapi.service;

import com.devops.cicdtoolapi.model.BuildInfo;
import com.devops.cicdtoolapi.model.CreateJobResult;
import com.devops.cicdtoolapi.model.JenkinsJobRequest;
import com.devops.cicdtoolapi.model.TriggerResult;
import com.devops.cicdtoolapi.exception.JenkinsJobAlreadyExistsException;
import com.devops.cicdtoolapi.exception.JenkinsResourceNotFoundException;
import com.devops.cicdtoolapi.exception.JenkinsServiceException;

public interface JenkinsService {

    /**
     * Retrieves the status of a specific Jenkins job build.
     * @param jobName The name of the Jenkins job.
     * @param buildNumber The specific build number, or null for the latest build.
     * @return BuildInfo containing the status and details.
     * @throws JenkinsResourceNotFoundException if the job or build doesn't exist.
     * @throws JenkinsServiceException for other Jenkins communication errors.
     */
    BuildInfo getJobBuildStatus(String jobName, Integer buildNumber);

    /**
     * Triggers a simple Jenkins build (without parameters).
     * @param jobName The name of the Jenkins job to trigger.
     * @return TriggerResult containing a confirmation message and queue URL.
     * @throws JenkinsResourceNotFoundException if the job doesn't exist.
     * @throws JenkinsServiceException for other Jenkins communication errors or trigger failures.
     */
    TriggerResult triggerSimpleBuild(String jobName);

     /**
     * Creates a new Jenkins pipeline job based on a template.
     * @param request Details of the job to create (e.g., job name).
     * @return CreateJobResult containing a confirmation message.
     * @throws JenkinsJobAlreadyExistsException if a job with the same name exists.
     * @throws JenkinsServiceException for errors during template loading or job creation.
     */
    CreateJobResult createPipelineJob(JenkinsJobRequest request);

}