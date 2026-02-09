package com.brimmatech.docflow.common.email;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailRequest {

    @JsonProperty("to_email")
    private List<String> toList;
    @JsonProperty("cc_email")
    private List<String> ccList;
    @JsonProperty("to_contact_name")
    private List<String> toContactList;
    private Map<String, Object> model;
    private String subject;
    private String textContent;
    private List<File> attachments;
    @JsonProperty("bcc_email")
    private List<String> bccList;
    private EmailRx.EmailTopic emailTopic;

}