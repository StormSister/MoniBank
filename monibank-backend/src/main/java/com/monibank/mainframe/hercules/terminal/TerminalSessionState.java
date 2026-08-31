package com.monibank.mainframe.hercules.terminal;

public enum TerminalSessionState {
    DISCONNECTED,
    CONNECTING,
    LOGGING_IN,
    STARTING_KICKS,
    OPENING_MBGW,
    READY,
    BUSY,
    RECOVERING,
    CLOSING,
    CLOSED,
    FAILED
}