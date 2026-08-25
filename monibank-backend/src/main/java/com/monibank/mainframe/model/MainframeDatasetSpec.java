package com.monibank.mainframe.model;

public record MainframeDatasetSpec(
        String ddName,
        String datasetName,
        MainframeDatasetMode mode
) {
}