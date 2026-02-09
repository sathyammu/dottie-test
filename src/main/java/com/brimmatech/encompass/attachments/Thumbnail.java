package com.brimmatech.encompass.attachments;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Thumbnail{
	private String url;
}