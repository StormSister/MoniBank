package com.monibank.mainframe.model;

public enum MainframeDatasetMode {

    READ("SHR"),
    UPDATE("OLD");

    private final String disposition;

    MainframeDatasetMode(String disposition) {
        this.disposition = disposition;
    }

    public String disposition() {
        return disposition;
    }
}