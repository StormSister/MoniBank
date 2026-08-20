package com.monibank.mainframe.model;

public record MainframeOperationSpec(
        String operationName,
        String programName,
        String targetDataset,
        int inputRecordLength,
        int entityRecordLength,
        MainframeOperationType type
) {
}