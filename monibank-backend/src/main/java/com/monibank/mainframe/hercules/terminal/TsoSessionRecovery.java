package com.monibank.mainframe.hercules.terminal;

@FunctionalInterface
public interface TsoSessionRecovery {

    boolean cancel(String username);
}
