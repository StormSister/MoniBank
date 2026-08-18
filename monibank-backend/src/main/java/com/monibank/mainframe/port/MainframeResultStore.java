package com.monibank.mainframe.port;

import java.util.List;

public interface MainframeResultStore {

    List<String> read(String datasetName);

    void delete(String datasetName);
}