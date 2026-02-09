package com.brimmatech.docflow.v2.repository;

import java.time.LocalDate;

public interface ReportTaskAndTree {

    String getTopic();

    int getSequence();

    String getIdentifier();

    LocalDate getCreated();

    LocalDate getCompleted();

    String getInput();

    String getOutput();

    String getQualifier();

    int getTenantId();

    String getLevels();

    int getDepth();

    String getRoute();

    String getTreeRoot();


}
