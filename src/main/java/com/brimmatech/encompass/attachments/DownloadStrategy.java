package com.brimmatech.encompass.attachments;

import lombok.Getter;

public enum DownloadStrategy {

    BATCH_DOWNLOAD("BATCH_DOWNLOAD", "batch"),
    API_DOWNLOAD("API_DOWNLOAD","api");

    private final String name;


    @Getter
    private final String qualifier;

    DownloadStrategy(String name, String qualifier) {
        this.name = name;
        this.qualifier = qualifier;
    }
}
