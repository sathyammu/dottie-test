package com.brimmatech.encompass.lockHandler.encompasslockhandler;

import lombok.Data;

@Data
public class EncompassLockResourceRequest {
    private Resource resource;
    private String lockType;
}
