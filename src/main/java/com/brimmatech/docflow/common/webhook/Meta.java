package com.brimmatech.docflow.common.webhook;

import lombok.Data;

@Data
public class Meta{
	private String resourceRef;
	private String resourceId;
	private String instanceId;
	private Payload payload;
	private String userId;
	private String resourceType;
}