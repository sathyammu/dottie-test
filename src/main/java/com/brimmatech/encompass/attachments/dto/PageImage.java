package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class PageImage{
    private int dpiY;
    private int dpiX;
    private String zipKey;
    private String imageKey;
    private int width;
    private String originalKey;
    private int height;
}   