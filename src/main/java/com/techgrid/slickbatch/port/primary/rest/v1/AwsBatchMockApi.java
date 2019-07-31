package com.techgrid.slickbatch.port.primary.rest.v1;

import com.amazonaws.services.batch.model.*;
import com.techgrid.slickbatch.application.AwsBatchMockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

@RestController
@RequestMapping("/v1")
public class AwsBatchMockApi {
    @Autowired
    private AwsBatchMockService service;

    @PostMapping(value = "/submitjob", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<SubmitJobResult> submitJob(@RequestBody SubmitJobRequest request) {
        return ResponseEntity.ok(service.submitJob(request));
    }

    @PostMapping(value = "/listjobs", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<ListJobsResult> listJobs(@RequestBody ListJobsRequest request) {

        return ResponseEntity.ok(service.listJobs(request));
    }

    @PostMapping(value = "/describejobs", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<DescribeJobsResult> describeJob(@RequestBody DescribeJobsRequest request) {

        return ResponseEntity.ok(service.describeJob(request));
    }

    @PostMapping(value = "/terminatejob", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<TerminateJobResult> terminate(@RequestBody TerminateJobRequest request) {

        return ResponseEntity.ok(service.terminate(request));
    }
}
