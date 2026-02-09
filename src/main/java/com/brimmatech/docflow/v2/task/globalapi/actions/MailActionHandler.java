package com.brimmatech.docflow.v2.task.globalapi.actions;

import com.brimmatech.docflow.common.email.AppEmailService;
import com.brimmatech.docflow.common.email.EmailRequest;
import com.brimmatech.docflow.common.email.EmailRx;
import com.brimmatech.docflow.common.email.MailServer;
import lombok.AllArgsConstructor;
import no.skatteetaten.fastsetting.formueinntekt.felles.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class MailActionHandler implements ActionHandler {

    private final AppEmailService emailService;
    private final MailServer mailServer;

    @Override
    public ActionResult perform(String actionSpec, Task task, Map<String, Object> context) {

        ActionResult actionResult;
        if (actionSpec == null || actionSpec.isEmpty()) {
            actionResult = ActionResult.FAILURE;
        } else {

            Optional<EmailRx> globalActionItems = emailService.retrieveGlobalActionItems();

            if(globalActionItems.isPresent()) {

                EmailRx emailRx = globalActionItems.get();
                Map<String, ActionTopicDto> actionItems = emailRx.getActionItems();
                ActionTopicDto actionTopicDto = actionItems.get(actionSpec);

                EmailRequest emailRequest = EmailRequest.builder().toList(actionTopicDto.getTo())
                        .ccList(actionTopicDto.getCc())
                        .bccList(actionTopicDto.getBcc())
                        .subject(actionTopicDto.getSubject())
                        .textContent(actionTopicDto.getBody())
                        .build();
                try {
                    emailService.sendEmail(mailServer, emailRequest);
                    actionResult = ActionResult.SUCCESS;
                } catch (Exception e) {
                    actionResult = ActionResult.FAILURE;
                }
            } else {
                actionResult = ActionResult.FAILURE;
            }

        }

        return actionResult;

    }


    @SuppressWarnings("unused")
    private String applyTemplate(String template, Map<String, Object> context) {
        if (template == null || context == null) return template;
        String result = template;
        for (Map.Entry<String, Object> e : context.entrySet()) {
            String placeholder = "{{" + e.getKey() + "}}";
            result = result.replace(placeholder, String.valueOf(e.getValue()));
        }
        return result;
    }
}
