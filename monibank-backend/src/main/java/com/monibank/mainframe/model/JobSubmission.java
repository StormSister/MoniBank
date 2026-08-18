package com.monibank.mainframe.model;

public record JobSubmission(
        String jobName,
        JobStatus status
) {
}