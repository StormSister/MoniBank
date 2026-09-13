package com.monibank.mainframe.operations;

@FunctionalInterface
public interface OperationEventPublisher {

    void publish(CoreOperationEvent event);
}
