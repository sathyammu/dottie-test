package com.brimmatech.docflow.v2.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter public class TenantSettingsMetaAction {
    private String type;
    private Boolean manualTrigger;
}
