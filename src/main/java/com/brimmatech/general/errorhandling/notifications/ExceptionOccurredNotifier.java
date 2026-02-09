package com.brimmatech.general.errorhandling.notifications;

import com.brimmatech.docflow.common.email.AppEmailService;
import com.brimmatech.docflow.common.email.EmailRequest;
import com.brimmatech.docflow.common.email.EmailRx;
import com.brimmatech.docflow.common.email.MailServer;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.general.types.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service @Slf4j @RequiredArgsConstructor public class ExceptionOccurredNotifier {
    private final TemplateEngine templateEngine;
    private final AppEmailService appEmailService;
    private final MailServer mailServer;


    public void notifyException(Exception e, TenantEntity requestingTenant, String requestPath, String requestParams) {
        try {
            EmailRequest request = new EmailRequest();
            val
                    rx =
                    appEmailService.setupRx(-1L,
                            EmailRx.EmailTopic.BRIMMA_GLOBAL_SUPPORT,
                            request,
                            String.format("Exception Occurred: %s", requestingTenant.getTenantName()));
            if (rx.isEmpty()) {
                return;
            }


            Context context = new Context();
            context.setVariable("exceptionMessage", e.getMessage());
            context.setVariable("tenantName", requestingTenant.getTenantName());
            context.setVariable("tenantId", requestingTenant.getId());
            context.setVariable("timestamp", java.time.LocalDateTime.now());
            context.setVariable("stackTrace", getStackTraceAsString(e));
            context.setVariable("requestParams", requestParams);
            context.setVariable("requestPath", requestPath);

            String emailContent = templateEngine.process("support/exception-notifier", context);

            request.setTextContent(emailContent);
            appEmailService.sendEmail(mailServer, request);
        } catch (Exception ex) {
            log.error("Unable to send invalid password notification", e);
        }
    }

    private String getStackTraceAsString(Exception ex) {
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : ex.getStackTrace()) {
            stackTrace.append(element.toString()).append("\n");
        }
        return stackTrace.toString();
    }
}
