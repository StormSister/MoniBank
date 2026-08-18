package com.monibank.mainframe.port;

public interface MainframeGateway {

    boolean isAvailable();

    String submitJcl(String jcl);
}