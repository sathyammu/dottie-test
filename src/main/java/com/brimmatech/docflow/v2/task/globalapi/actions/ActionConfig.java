package com.brimmatech.docflow.v2.task.globalapi.actions;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class ActionConfig {

    private final ActionExecutor actionExecutor;
    private final MailActionHandler mailHandler;

    public ActionConfig(ActionExecutor actionExecutor, MailActionHandler mailHandler) {
        this.actionExecutor = actionExecutor;
        this.mailHandler = mailHandler;
    }

    @PostConstruct
    public void init() {
        actionExecutor.register("mail", mailHandler);
    }
}
