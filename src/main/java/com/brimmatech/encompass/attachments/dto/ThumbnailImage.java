package com.brimmatech.encompass.attachments.dto;

import lombok.Data;

@Data
public class ThumbnailImage{
    private int dpiY;
    private int dpiX;
    private String zipKey;
    private String imageKey;
    private int width;
    private int height;
}