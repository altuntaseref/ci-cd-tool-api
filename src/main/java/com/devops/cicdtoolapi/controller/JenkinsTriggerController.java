package com.devops.cicdtoolapi.controller;

import com.devops.cicdtoolapi.model.BuildInfo;
import com.devops.cicdtoolapi.model.CreateJobResult;
import com.devops.cicdtoolapi.model.JenkinsJobRequest;
import com.devops.cicdtoolapi.model.TriggerResult;
import com.devops.cicdtoolapi.service.JenkinsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/jenkins")
public class JenkinsTriggerController {

    private static final Logger logger = LoggerFactory.getLogger(JenkinsTriggerController.class);

    private final JenkinsService jenkinsService; // Inject the interface

    public JenkinsTriggerController(JenkinsService jenkinsService) {
        this.jenkinsService = jenkinsService;
    }

    /**
     * Gets the status of a specific or latest build for a Jenkins job.
     */
    @GetMapping("/status/{jobName}")
    public ResponseEntity<BuildInfo> getJobStatus(
            @PathVariable String jobName,
            @RequestParam(required = false) Integer buildNumber) {
        BuildInfo buildInfo = jenkinsService.getJobBuildStatus(jobName, buildNumber);
        return ResponseEntity.ok(buildInfo);
    }

    /**
     * Triggers a simple build for the specified Jenkins job.
     */
    @PostMapping("/trigger/{jobName}")
    public ResponseEntity<TriggerResult> triggerJob(@PathVariable String jobName) {
        TriggerResult result = jenkinsService.triggerSimpleBuild(jobName);
        return ResponseEntity.accepted().body(result);
    }

    /**
     * Creates a new Jenkins pipeline job.
     */
    @PostMapping("/create/pipeline")
    public ResponseEntity<CreateJobResult> createPipelineJob(@RequestBody JenkinsJobRequest request) {
        if (request == null || request.getJobName() == null || request.getJobName().isBlank()) {
            return ResponseEntity.badRequest().body(new CreateJobResult("Job name must be provided."));
        }
        CreateJobResult result = jenkinsService.createPipelineJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }


}