package com.brimmatech.docflow.v2.dto.jpa;

import java.time.LocalDate;

public interface TaskAndTreeDto {
    public Integer getId();

    public String getQualifier();

    public String getLevels();
    public int  getNumLevels();

    public String getMeta();

    public String getTopic();

    public Integer getSequence();

    public String getIdentifier();

    public Boolean getRecreated();

    public LocalDate getCreated();

    public LocalDate getCompleted();

    public String getState();

    public String getInput();

    public String getOutput();

}
