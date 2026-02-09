package com.brimmatech.docflow.common.email;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.general.utils.OptionUtils;
import jakarta.mail.*;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static jakarta.mail.Message.RecipientType.*;

@Configuration @Slf4j @RequiredArgsConstructor public class AppEmailService {
    private final static String OUTLOOK_EMAIL_PROVIDER = "OUTLOOK";
    private final TenantSettingsService tenantSettingsService;
    @Value("${info.app.deploy_environment}") private String appDeployEnv;

    public void sendEmail(MailServer mailServer, EmailRequest request) throws MessagingException {
        Properties propvls = setupJMSPropertiesForSMTP(mailServer);
        Session sessionobj = Session.getInstance(propvls, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(mailServer.getUserName(), mailServer.getPassword());
            }
        });
        MimeMessage message = new MimeMessage(sessionobj);
        MimeMessageHelper
                helper =
                new MimeMessageHelper(message,
                        MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                        StandardCharsets.UTF_8.name());
        helper.setText(request.getTextContent(), true);
        helper.setSubject(request.getSubject());
        helper.setFrom(mailServer.getUserName());

        if (null != request.getAttachments()) {
            request.getAttachments().forEach(attachment -> {
                try {
                    helper.addAttachment(attachment.getName(), attachment);
                } catch (MessagingException exception) {
                    log.error("Error while attempting to attach a file {}", exception.getMessage());
                }
            });
        }
        List<String>
                toList =
                request.getToList().stream().filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());

        if (null != request.getCcList() && !request.getCcList().isEmpty()) {
            List<String>
                    ccList =
                    request.getCcList().stream().filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
            message.addRecipients(CC, getAddressArray(ccList));
        }
        List<String> bccEmailList = request.getBccList();
        if (bccEmailList != null && !bccEmailList.isEmpty()) {
            List<String>
                    bccList =
                    bccEmailList.stream().filter(item -> !item.trim().isEmpty()).collect(Collectors.toList());
            message.addRecipients(BCC, getAddressArray(bccList));
        }
        if (!toList.isEmpty()) {
            message.addRecipients(TO, getAddressArray(toList));
        }
        log.info("Sending mail for topic :{} to {}", request.getEmailTopic(),message.getAllRecipients());


        Transport.send(message);
        log.info("Email sent successfully");
    }

    private Address[] getAddressArray(List<String> addressList) {
        Address[] addresses = new InternetAddress[addressList.size()];
        int index = 0;
        for (String address : addressList) {
            try {
                addresses[index] = new InternetAddress(address);
                index++;
            } catch (AddressException ae) {
                log.error("AddressException", ae);
            }
        }
        return addresses;
    }

    private Properties setupJMSPropertiesForSMTP(MailServer server) {
        Properties properties = new Properties();
        properties.put(String.format("mail.%s.host", "smtp"),
                server.getSmtpHost());
        properties.put(String.format("mail.%s.port", "smtp"),
                server.getSmtpPort());
        properties.put(String.format("mail.%s.auth", "smtp"), true);
        properties.put(String.format("mail.%s.starttls.enable", "smtp"), true);
        if (!OUTLOOK_EMAIL_PROVIDER.equals(server.getMailProvider())) {
            properties.put(String.format("mail.%s.ssl.trust", "smtp"),
                    server.getSmtpHost());
            properties.put(String.format("mail.%s.starttls.required", "smtp"), "true");
            properties.put(String.format("mail.%s.socketFactory.class", "smtp"),
                    "javax.net.ssl.SSLSocketFactory");
        }
        properties.put(String.format("mail.%s.ssl.protocols", "smtp"),
                "TLSv1.2");
        properties.put(String.format("mail.%s.connectiontimeout", "smtp"),
                "100000");
        properties.put(String.format("mail.%s.timeout", "smtp"),
                "100000");
        properties.put(String.format("mail.%s.ssl.writetimeout", "smtp"),
                "100000");
        return properties;
    }

    public Optional<EmailRx.Rx> setupRx(long tenantId, EmailRx.EmailTopic topic, EmailRequest request, String subject) {
        Optional<EmailRx>
                tenantRx =
                tenantSettingsService.getSettingTyped(tenantId, SettingsCategory.EMAIL_RX, EmailRx.class);
        Optional<EmailRx>
                globalRx =
                tenantSettingsService.getGlobalSettingTyped(SettingsCategory.EMAIL_RX, EmailRx.class);

        Optional<EmailRx> mergedEmailRx = OptionUtils.merge(tenantRx, globalRx, EmailRx::mergeWith);

        request.setEmailTopic(topic);

        Optional<EmailRx.Rx>
                rxMerged =
                topic.isShouldApplyGlobals() ?
                        OptionUtils.merge(mergedEmailRx.map(v -> v.topicRecipients.get(topic)),
                                mergedEmailRx.map(v -> v.topicRecipients.get(EmailRx.EmailTopic.BRIMMA_GLOBAL_SUPPORT)),
                                EmailRx.Rx::merge) :
                        tenantRx.map(v -> v.topicRecipients.get(topic));


        if (rxMerged.isEmpty()) {
            log.debug("No recipients configured for {}. Not sending email", topic);
        }
        rxMerged.ifPresent(rxResolved -> rxResolved.populateRx(request));
        request.setSubject(String.format("Orchestrator(%s):%s", appDeployEnv, subject));
        return rxMerged;
    }


    public Optional<EmailRx> retrieveGlobalActionItems() {

         return tenantSettingsService.getGlobalSettingTyped(SettingsCategory.EMAIL_RX, EmailRx.class);
    }
}
