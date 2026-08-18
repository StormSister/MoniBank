package com.monibank.mainframe.model;

public record MainframeOperationSpec(
        String operationName,
        String programName,
        String targetDataset,
        int recordLength,
        MainframeOperationType type
) {
}