package com.brimmatech.docflow.common.beanhelper;

import com.brimmatech.encompass.attachments.AttachmentProcessor;
import com.brimmatech.encompass.conditions.ICondition;
import com.brimmatech.encompass.documentcreator.DocumentProcessor;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanupdater.ILoanUpdater;
import com.brimmatech.encompass.lockHandler.LoanLockHandler;
import com.brimmatech.encompass.tokengenerator.TokenGenerator;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BeanHelper {

    private ApplicationContext applicationContext;

    public TokenGenerator getTokenGenerator(String name){
        return applicationContext.getBean(name.toLowerCase(), TokenGenerator.class);
    }

    public LoanReader getLoanReaderBean(String name){
        return applicationContext.getBean(name.toLowerCase(), LoanReader.class);
    }

    public DocumentProcessor getDocumentProcessor(String name){
        return applicationContext.getBean(name.toLowerCase(), DocumentProcessor.class);
    }

    public LoanLockHandler getLoanLockHandler(String name){
        return applicationContext.getBean(name.toLowerCase(), LoanLockHandler.class);
    }

    public AttachmentProcessor getAttachmentProcessor(String name){
        return applicationContext.getBean(name.toLowerCase(), AttachmentProcessor.class);
    }

    public ILoanUpdater getLoanUpdaterBean(String name) {
        return applicationContext.getBean(name.toLowerCase(), ILoanUpdater.class);
    }


    public ICondition getLOSConditionUpdaterBean(String name) {
        return applicationContext.getBean(name.toLowerCase(), ICondition.class);
    }
}