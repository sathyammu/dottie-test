package com.brimmatech.encompass.loanupdater;

import com.brimmatech.docflow.v2.dto.LoanUpdateDTO;
import com.brimmatech.docflow.v2.dto.PurchaseAdviceDTO;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface ILoanUpdater {

    boolean updateLoanByFields(PurchaseAdviceDTO fieldsToBeUpdate, String loanIdentifier, String accessToken);

    String updateLoanFields(Map<String,List<LoanUpdateDTO>> updates, String accessToken);
}
