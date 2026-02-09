package com.brimmatech.encompass.loanreader.encompass.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Asset {
    private String assetType;
    private boolean isVod;
    private String id;
    private String accountIdentifier;
    private String altId;
    private int assetIndex;
    private String depositoryAccountName;
    private String holderName;
    private boolean noLinkToDocTrackIndicator;
    private String owner;
    private boolean printAttachmentIndicator;
    private String titlePhone;
    private double total;
    private int vodIndex;
    private double urla2020CashOrMarketValueAmount;
    private String depositoryAccountGuid;
    private boolean includeInAusExport;
    private String sourceOfAssetData;
}
