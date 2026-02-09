package com.brimmatech.encompass.attachments.dto;


import lombok.Data;

import java.util.List;

@Data
public class EncompassAttachmentV3Response{
    private boolean isRemoved;
    private List<PagesItem> pages;
    private String createdDate;
    private int fileSize;
    private CreatedBy createdBy;
    private String id;
    private String title;
    private String type;
    private CreatedBy assignedTo;
}
