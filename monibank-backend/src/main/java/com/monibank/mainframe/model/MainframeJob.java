package com.monibank.mainframe.model;

public record MainframeJob(
        String jobName,
        String jobId,
        JobStatus status
) {
}