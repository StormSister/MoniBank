package com.monibank.mainframe.model;

import java.util.List;

public record MainframeOperationSpec(
        String operationName,
        String programName,
        String targetDataset,
        int inputRecordLength,
        int entityRecordLength,
        MainframeOperationType type,
        List<MainframeDatasetSpec> datasets
) {
}