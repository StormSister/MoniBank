package com.monibank.mainframe.port;

import java.io.IOException;

public interface MainframeLiveLogProcessFactory {

    Process start(int initialLines) throws IOException;
}
