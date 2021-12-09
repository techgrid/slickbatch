package com.techgrid.slickbatch.port.primary.rest.v1;

import com.amazonaws.services.batch.model.*;
import com.techgrid.slickbatch.application.AwsBatchMockService;
import com.techgrid.slickbatch.logging.SBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping("/v1")
public class AwsBatchMockApi {
    @Autowired
    private AwsBatchMockService service;

    @PostMapping(value = "/submitjob", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<SubmitJobResult> submitJob(@RequestBody SubmitJobRequest request) {
        try {
            SBLogger.info("Submitting job: " + request.getJobName() + ", Queue: " + request.getJobQueue());
            return ResponseEntity.ok(service.submitJob(request));
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/listjobs", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ListJobsResult> listJobs(@RequestBody ListJobsRequest request) {
        try {
            SBLogger.info(String.format("Processing list job request for queue: %s", request.getJobQueue()));
            return ResponseEntity.ok(service.listJobs(request));
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(value = "/describejobs", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DescribeJobsResult> describeJob(@RequestBody DescribeJobsRequest request) {
        try {
            SBLogger.info("Processing describe job request for: " + String.join(", ", request.getJobs()));
            return ResponseEntity.ok(service.describeJob(request));
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping(value = "/terminatejob", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminateJobResult> terminate(@RequestBody TerminateJobRequest request) {
        try {
            SBLogger.info("Processing terminate requests for: " + request.getJobId());
            return ResponseEntity.ok(service.terminate(request));
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "jobs/{jobId}/delete", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable("jobId") String jobId) {
        try {
            SBLogger.info("Processing delete requests for: " + jobId);
            service.delete(jobId);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "jobs/{jobId}/status", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DescribeJobsResult> delete(@PathVariable("jobId") String jobId, @RequestBody String status) {
        try {
            SBLogger.info("Processing status requests for: " + jobId);
            return ResponseEntity.ok(service.updateStatus(jobId, status));
        } catch (Exception ex) {
            SBLogger.error(ex.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
