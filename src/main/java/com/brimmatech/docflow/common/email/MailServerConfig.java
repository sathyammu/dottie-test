package com.brimmatech.docflow.common.email;


import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MailServerConfig {

    private EmailConfigProperties emailConfigProperties;

    @Bean
    MailServer mailServer() {

        MailServer mailServer = new MailServer();
        mailServer.setSmtpAuth(emailConfigProperties.getSmtpAuthEnabled());
        mailServer.setSmtpTLSEnable(emailConfigProperties.getSmtpTlsEnabled());
        mailServer.setSmtpHost(emailConfigProperties.getHostname());
        mailServer.setSmtpPort(Integer.parseInt(emailConfigProperties.getPort()));
        mailServer.setUserName(emailConfigProperties.getUsername());
        mailServer.setPassword(emailConfigProperties.getPassword());
        mailServer.setMailProvider(emailConfigProperties.getProvider());

        return mailServer;
    }
}