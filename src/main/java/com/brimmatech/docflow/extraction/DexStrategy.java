package com.brimmatech.docflow.extraction;

import lombok.Getter;

public enum DexStrategy {

    DOC_INTEL("DOC_INTEL", "dex:doc-intell"),
    CULMINATE("CULMINATE", "dex:culminate"),
    CONTENT_UNDERSTANDING("CONTENT_UNDERSTANDING", "dex:content-understanding"),
        REGEX_IMAGE_BASED("REGEX_IMAGE_BASED", "dex:content-understanding"),
    MULTI_LEVEL_EXTRACT("MLE", "dex:mle");

    private final String name;


    @Getter
    private final String qualifier;

    DexStrategy(String name, String qualifier) {
        this.name = name;
        this.qualifier = qualifier;
    }

}