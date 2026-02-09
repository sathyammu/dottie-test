package com.brimmatech.docflow.common.email;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "mail")
public class EmailConfigProperties {

    private String hostname;
    private String port;
    private String username;
    private String password;
    private String sslProtocol;
    private String smtpAuthEnabled;
    private String smtpTlsEnabled;
    private List<String> toAddresses;
    private List<String> ccAddresses;
    private List<String> serviceEmailAddresses;
    private String subject;
    private String provider;
    private String tlsProtocol;
    private String connectionTimeout;

}
