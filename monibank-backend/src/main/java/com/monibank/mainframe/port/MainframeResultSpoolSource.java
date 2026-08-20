package com.monibank.mainframe.port;

import java.util.List;

public interface MainframeResultSpoolSource {

    List<String> readRecentLines(int numberOfLines);
}