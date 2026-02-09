package com.brimmatech.docflow.common.queueprocessor;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessMessage {

    @JsonProperty("eventId")
    private String eventId;

    @JsonProperty("loanIdentifier")
    private String loanIdentifier;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("webhookArgs")
    private Map<String, String> webhookArgs;

    @JsonProperty("eventTime")
    private String eventTime;

    @JsonProperty("eventType")
    private TenantSettingsMeta.TaskTriggeringEventType eventType;

    @JsonProperty("flowName")
    private TenantSettingsMeta.TaskBusinessFlowName flowName;

    @JsonProperty("document_meta")
    private JsonNode documentMeta;
}
