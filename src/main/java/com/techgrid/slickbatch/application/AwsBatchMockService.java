package com.techgrid.slickbatch.application;

import com.amazonaws.services.batch.model.*;
import com.techgrid.slickbatch.logging.SBLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class AwsBatchMockService {
    private static final Random random = new Random();
    private static final int account = 500000000 + random.nextInt(99999999);
    private static final List<String> STATUSES = List.of("SUBMITTED", "PENDING", "RUNNABLE", "STARTING", "RUNNING", "FAILED", "SUCCEEDED");
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final List<String> markedForDeletion = new ArrayList<>();

    private static final Map<String, String> NEXT_STATE = Map.of(
            "SUBMITTED", "PENDING",
            "PENDING", "RUNNABLE",
            "RUNNABLE", "STARTING",
            "STARTING", "RUNNING",
            "RUNNING", "SUCCEEDED",
            "SUCCEEDED", "DELETED",
            "FAILED", "DELETED");

    private static final Map<String, JobDetail> jobDetails = new HashMap<>();
    private static final Map<String, String> jobQueue = new HashMap<>();
    private static final Map<String, Long> stateTransitionTimestamp = new HashMap<>();

    private static final Map<String, Consumer<JobDetail>> stateTransitionMap = Map.of(
            "SUBMITTED", jd -> {
                jd.setStatus("SUBMITTED");
                jd.setCreatedAt(System.currentTimeMillis());
            },
            "PENDING",  jd -> {
                jd.setStatus("PENDING");
            },
            "RUNNABLE",  jd -> {
                jd.setStatus("RUNNABLE");
            },
            "STARTING",  jd -> {
                jd.setStatus("STARTING");
                jd.setStartedAt(System.currentTimeMillis());
            },
            "RUNNING",  jd -> {
                jd.setStatus("RUNNING");
            },
            "FAILED",  jd -> {
                jd.setStatus("FAILED");
                jd.setStoppedAt(System.currentTimeMillis());
                jd.setStatusReason("Essential container in task exited");
                if (RANDOM.nextInt(100) > 90)
                    jd.getAttempts().get(0).setStatusReason("Container exceeded memory utilization");
            },
            "SUCCEEDED", jd -> {
                jd.setStatus("SUCCEEDED");
                jd.setStoppedAt(System.currentTimeMillis());
                jd.setStatusReason("Container task exited successfully");
            },
            "DELETED", jd -> markedForDeletion.add(jd.getJobId())
    );


    @Autowired
    private StageWaitTimes stageWaitTimes;

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
                .withAttempts(new AttemptDetail())
                .withCreatedAt(System.currentTimeMillis())
                .withParameters(request.getParameters())
                .withTimeout(request.getTimeout());
        jobDetails.put(job.getJobId(), job);
        jobQueue.put(job.getJobId(), request.getJobQueue());

        stateTransitionTimestamp.put(job.getJobId(), job.getCreatedAt());

        SBLogger.success("Job (" + job.getJobName() + ") created with id: " + job.getJobId());
        return new SubmitJobResult().withJobId(job.getJobId()).withJobName(request.getJobName());
    }

    public ListJobsResult listJobs(ListJobsRequest request) {
        if (request.getJobStatus() == null)
            request.setJobStatus("RUNNING");

        return new ListJobsResult()
                .withJobSummaryList(
                        jobDetails.values().stream()
                                .filter(jd -> filterJobDetails(jd, request))
                                .map(this::mapToSummary)
                                .collect(Collectors.toList()));
    }

    private boolean filterJobDetails(JobDetail jd, ListJobsRequest request) {
        return jobQueue.get(jd.getJobId()).equals(request.getJobQueue()) &&
                (jd.getStatus().equals(request.getJobStatus()) || (
                    null != request.getFilters() && request.getFilters().stream()
                            .filter(k -> "JOB_NAME".equals(k.getName()))
                            .map(KeyValuesPair::getValues)
                            .anyMatch(names -> names.contains(jd.getJobName()))));
    }

    public DescribeJobsResult describeJob(DescribeJobsRequest request) {
        return new DescribeJobsResult()
                .withJobs(jobDetails.values()
                        .stream()
                        .filter(jd -> request.getJobs().contains(jd.getJobId()))
                        .collect(Collectors.toList()));
    }

    public TerminateJobResult terminate(TerminateJobRequest request) {
        jobDetails.values()
                .stream()
                .filter(jd -> jd.getJobId().equals(request.getJobId()))
                .forEach(jd -> {
                    stateTransitionMap.get("FAILED").accept(jd);
                    jd.setStatusReason(request.getReason());
                    SBLogger.success("Job with id " + jd.getJobId() + " terminated successfully");
                });
        return new TerminateJobResult();
    }

    public List<JobSummary> allJobs() {
        return new ArrayList<>(summaries());
    }

    @Scheduled(fixedDelay = 1000)
    public void changeState() {
        jobDetails.values().forEach(jd -> {
            if (NEXT_STATE.containsKey(jd.getStatus())) {
                if (!stateTransitionTimestamp.containsKey(jd.getJobId())) {
                    stateTransitionTimestamp.put(jd.getJobId(), System.currentTimeMillis());
                    return;
                }
                if ((System.currentTimeMillis() - stateTransitionTimestamp.get(jd.getJobId())) > stageWaitTimes.getWaitMs().getOrDefault(jd.getStatus(), 5000)) {
                    SBLogger.info("Job " + jd.getJobId() + " is being moved from " + jd.getStatus() + " to " + NEXT_STATE.get(jd.getStatus()));
                    stateTransitionMap.get(NEXT_STATE.get(jd.getStatus())).accept(jd);
                    stateTransitionTimestamp.put(jd.getJobId(), System.currentTimeMillis());
                }
            }
        });
        markedForDeletion.forEach(jobDetails::remove);
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

    public void delete(String jobId) {
        var jobIds = getJobIdFromName(jobId);
        if (jobIds.isEmpty()) {
            SBLogger.error("Job with id/name " + jobId + " not found");
            return;
        }
        jobIds.forEach(id -> {
            jobDetails.remove(id);
            SBLogger.success("Job with id " + id + " was deleted");
        });
    }

    private List<String> getJobIdFromName(String jobId) {
        if (jobDetails.containsKey(jobId)) {
            return List.of(jobId);
        }
        return jobDetails.entrySet().stream().filter(e -> e.getValue().getJobName().equals(jobId)).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public DescribeJobsResult updateStatus(String jobId, String status) {
        var jobIds = getJobIdFromName(jobId);
        if (jobIds.isEmpty()) {
            SBLogger.error("Job with id/name " + jobId + " not found");
            return new DescribeJobsResult();
        }

        jobIds.forEach(id -> {
            if (!STATUSES.contains(status))
                SBLogger.error("Invalid Status " + status + " specified for job: " + id);

            stateTransitionMap.get(status).accept(jobDetails.get(id));
            stateTransitionTimestamp.put(id, System.currentTimeMillis());

            SBLogger.success("Job with id " + id + " moved to " + status);
        });

        return new DescribeJobsResult()
                .withJobs(jobDetails.entrySet()
                        .stream()
                        .filter(jd -> jobIds.contains(jd.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toList()));
    }

    private List<JobSummary> summaries() {
        return jobDetails.values().stream().map(this::mapToSummary).collect(Collectors.toList());
    }

    private JobSummary mapToSummary(JobDetail jd) {
        return new JobSummary()
                .withJobId(jd.getJobId())
                .withJobName(jd.getJobName())
                .withCreatedAt(jd.getCreatedAt())
                .withStartedAt(jd.getStartedAt())
                .withStoppedAt(jd.getStoppedAt())
                .withStatus(jd.getStatus())
                .withStatusReason(jd.getStatusReason());
    }
}
