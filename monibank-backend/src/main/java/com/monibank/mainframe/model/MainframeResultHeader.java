package com.monibank.mainframe.model;

public record MainframeResultHeader(
        String type,
        String operation,
        String code,
        String entityId,
        String status
) {
}