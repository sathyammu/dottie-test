package com.brimmatech.docflow.common.webhook;

import com.brimmatech.docflow.v2.dto.TenantSettingsMeta;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class WebhookDto{
	private String eventId;
	private Meta meta;
	private String eventTime;
	private TenantSettingsMeta.TaskTriggeringEventType eventType;
}