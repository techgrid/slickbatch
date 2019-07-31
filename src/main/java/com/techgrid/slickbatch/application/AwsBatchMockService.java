package com.techgrid.slickbatch.application;

import com.amazonaws.services.batch.model.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AwsBatchMockService {
    private static final Random random = new Random();
    private static int account = 500000000 + random.nextInt(99999999);

    private static final Map<String, JobDetail> jobDetails = new HashMap<>();
    private static final Map<String, JobSummary> jobSummaries = new HashMap<>();

    public SubmitJobResult submitJob(SubmitJobRequest request) {

        var job = new JobDetail()
                .withJobDefinition(jobDefinitionArn(request.getJobDefinition()))
                .withJobQueue(jobQueueArn(request.getJobQueue()))
                .withJobId(UUID.randomUUID().toString())
                .withJobName(request.getJobName())
                .withDependsOn(request.getDependsOn())
                .withContainer(
                        new ContainerDetail()
                        .withImage(containerImage())
                        .withEnvironment(request.getContainerOverrides().getEnvironment())
                        .withVcpus(request.getContainerOverrides().getVcpus())
                        .withCommand(request.getContainerOverrides().getCommand())
                        .withMemory(request.getContainerOverrides().getMemory())
                        .withInstanceType(request.getContainerOverrides().getInstanceType())
                        .withContainerInstanceArn(containerInstanceArn())
                        .withTaskArn(containerTaskArn())
                        .withResourceRequirements(request.getContainerOverrides().getResourceRequirements())
                        .withLogStreamName(logStreamName(request.getJobQueue()))

                ).withStatus("SUBMITTED")
                .withCreatedAt(System.currentTimeMillis())
                .withParameters(request.getParameters())
                .withTimeout(request.getTimeout());
        jobDetails.put(job.getJobId(), job);

        var summary = new JobSummary()
                .withJobId(job.getJobId())
                .withJobName(job.getJobName())
                .withCreatedAt(job.getCreatedAt())
                .withStatus(job.getStatus());

        jobSummaries.put(job.getJobId(), summary);

        return new SubmitJobResult().withJobId(job.getJobId()).withJobName(request.getJobName());
    }

    public ListJobsResult listJobs(ListJobsRequest request) {
        if (request.getJobStatus() == null)
            request.setJobStatus("RUNNING");
        return new ListJobsResult()
                .withJobSummaryList(jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals(request.getJobStatus()) && jd.getValue().getJobQueue().contains(request.getJobQueue()))
                .map(jd -> jobSummaries.get(jd.getKey()))
                .collect(Collectors.toList()));
    }

    public DescribeJobsResult describeJob(DescribeJobsRequest request) {
        return new DescribeJobsResult()
                .withJobs(jobDetails.entrySet()
                        .stream()
                        .filter(jd -> request.getJobs().contains(jd.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList()));
    }

    public TerminateJobResult terminate(TerminateJobRequest request) {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getJobId().equals(request.getJobId()))
                .forEach(jd -> {
                    long stopped = System.currentTimeMillis();
                    jd.getValue().setStatus("SUCCESS");
                    jd.getValue().setStoppedAt(stopped);
                    jd.getValue().setStatusReason(request.getReason());
                    jobSummaries.get(jd.getKey()).setStatus("SUCCESS");
                    jobSummaries.get(jd.getKey()).setStoppedAt(stopped);
                    jobSummaries.get(jd.getKey()).setStatusReason(request.getReason());
                });
        return new TerminateJobResult();
    }

    public List<JobSummary> allJobs() {
        return new ArrayList<>(jobSummaries.values());
    }

    @Scheduled(fixedDelay = 5000)
    public void moveToPending() {
        var expired = new ArrayList<>();
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("SUBMITTED"))
                .forEach(jd -> {
                    if (jd.getValue().getStoppedAt() != null && System.currentTimeMillis() - jd.getValue().getStoppedAt() > 24 * 60 * 60 * 1000 )
                        expired.add(jd.getKey());
                    jd.getValue().setStatus("PENDING");
                    jobSummaries.get(jd.getKey()).setStatus("PENDING");
                });
        expired.forEach(id -> {
            jobDetails.remove(id);
            jobSummaries.remove(id);
        });
    }

    @Scheduled(fixedDelay = 5000)
    public void moveToRunnable() {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("PENDING"))
                .forEach(jd -> {
                    jd.getValue().setStatus("RUNNABLE");
                    jobSummaries.get(jd.getKey()).setStatus("RUNNABLE");
                });
    }

    @Scheduled(fixedDelay = 30000)
    public void moveToStarting() {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("RUNNABLE"))
                .forEach(jd -> {
                    long started = System.currentTimeMillis();
                    jd.getValue().setStatus("STARTING");
                    jd.getValue().setStartedAt(started);
                    jobSummaries.get(jd.getKey()).setStatus("STARTING");
                    jobSummaries.get(jd.getKey()).setStartedAt(started);
                });
    }

    @Scheduled(fixedDelay = 30000)
    public void moveToRunning() {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("STARTING"))
                .forEach(jd -> {
                    jd.getValue().setStatus("RUNNING");
                });
    }

    @Scheduled(fixedDelay = 120000)
    public void moveToSuccess() {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("RUNNING"))
                .forEach(jd -> {
                    long stopped = System.currentTimeMillis();
                    jd.getValue().setStatus("SUCCESS");
                    jd.getValue().setStoppedAt(stopped);
                    jd.getValue().setStatusReason("Container task exited successfully");
                    jobSummaries.get(jd.getKey()).setStatus("SUCCESS");
                    jobSummaries.get(jd.getKey()).setStoppedAt(stopped);
                    jobSummaries.get(jd.getKey()).setStatusReason("Container task exited successfully");

                });
    }

    @Scheduled(fixedDelay = 210000)
    public void moveToFailure() {
        jobDetails.entrySet()
                .stream()
                .filter(jd -> jd.getValue().getStatus().equals("RUNNING"))
                .forEach(jd -> {
                    long stopped = System.currentTimeMillis();
                    jd.getValue().setStatus("FAILED");
                    jd.getValue().setStoppedAt(stopped);
                    jd.getValue().setStatusReason("Container task failed");
                    jobSummaries.get(jd.getKey()).setStatus("FAILED");
                    jobSummaries.get(jd.getKey()).setStoppedAt(stopped);
                    jobSummaries.get(jd.getKey()).setStatusReason("Container task failed");
                });
    }

    private String jobQueueArn(String queue) {
        return String.format("arn:aws:batch:us-east-1:%s:job-queue/%s", account, queue);
    }

    private String jobDefinitionArn(String definition) {
        return String.format("arn:aws:batch:us-east-1:%s:job-definition/%s", account, definition);
    }

    private String logStreamName(String definition) {
        return String.format("%s/default/%s", definition, UUID.randomUUID().toString());
    }

    private String containerInstanceArn() {
        return String.format("arn:aws:batch:us-east-1:%s:job-definition/%s", account, UUID.randomUUID().toString());
    }

    private String containerTaskArn() {
        return String.format("arn:aws:batch:us-east-1:%s:task/%s", account, UUID.randomUUID().toString());
    }

    private String containerImage() {
        return String.format("%s.dkr.ecr.us-east-1.amazonaws.com/user/test:GCC9-nexus-xpress-cpp", account);
    }

}
