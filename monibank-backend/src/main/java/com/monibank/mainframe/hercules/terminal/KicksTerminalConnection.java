package com.monibank.mainframe.hercules.terminal;

public interface KicksTerminalConnection extends AutoCloseable {

    TerminalSessionState state();

    void open() throws Exception;

    void verifyReady() throws Exception;

    MbgwTerminalResponse execute(MbgwRequest request)
            throws InterruptedException;

    @Override
    void close() throws Exception;
}
