package com.monibank.mainframe.model;

import java.util.List;

public record MainframeResult(
        MainframeResultHeader header,
        List<MainframeDataRecord> data
) {
}