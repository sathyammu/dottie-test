package com.brimmatech.encompass.loanreader.encompass.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vod {

    private String id;
    private String holderName;
    private boolean includeInAusExport;
    private boolean noLinkToDocTrackIndicator;
    private String owner;
    private String titlePhone;
    private double total;
    private String sourceOfAssetData;
    private boolean populatedContactIndicator;
    private List<VodItem> items;
    private String altId;
}
