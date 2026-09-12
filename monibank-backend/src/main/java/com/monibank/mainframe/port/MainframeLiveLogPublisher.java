package com.monibank.mainframe.port;

@FunctionalInterface
public interface MainframeLiveLogPublisher {

    void publishKicks(String rawLine);
}
