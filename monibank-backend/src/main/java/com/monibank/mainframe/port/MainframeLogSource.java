package com.monibank.mainframe.port;

import java.util.List;

public interface MainframeLogSource {

    List<String> readRecentLines(int numberOfLines);
}