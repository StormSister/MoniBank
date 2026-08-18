package com.monibank.mainframe.mapping;

public interface MainframeRecordMapper<T> {

    String toRecord(T request);
}