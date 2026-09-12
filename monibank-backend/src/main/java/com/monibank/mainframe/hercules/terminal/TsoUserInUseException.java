package com.monibank.mainframe.hercules.terminal;

public final class TsoUserInUseException
        extends IllegalStateException {

    private final String username;

    public TsoUserInUseException(String username) {
        super("TSO user " + username + " is already in use.");
        this.username = username;
    }

    public String username() {
        return username;
    }
}
