package com.brimmatech.docflow.common.email;

import com.brimmatech.docflow.v2.services.TenantSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantMailServerResolver {

    private final TenantSettingsService tenantSettingsService;
    private final MailServer mailServer;

    public MailServer resolveServer(long tenantId) {

        return tenantSettingsService.getMailServerDetails(tenantId)
                .map(config -> {
                    MailServer ms = new MailServer();
                    ms.setSmtpAuth(config.mailSmtpAuthEnabled());
                    ms.setSmtpTLSEnable(config.mailSmtpTlsEnabled());
                    ms.setSmtpHost(config.mailHost());
                    ms.setSmtpPort(Integer.parseInt(config.mailPort()));
                    ms.setUserName(config.mailUsername());
                    ms.setPassword(config.mailPassword());
                    ms.setMailProvider(config.mailProvider());
                    return ms;
                })
                .orElse(mailServer);
    }
}
