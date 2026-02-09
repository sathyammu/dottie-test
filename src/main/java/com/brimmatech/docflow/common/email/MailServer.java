package com.brimmatech.docflow.common.email;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
public class MailServer {

    private String userName;
    private String password;
    private String smtpAuth;
    private String smtpTLSEnable;
    private String smtpHost;
    private int smtpPort;
    private String mailProvider;

}