package com.brimmatech.docflow.common.webhook;

import lombok.Data;

@Data
public class Payload{
	private String correlationId;
	private Event event;
}