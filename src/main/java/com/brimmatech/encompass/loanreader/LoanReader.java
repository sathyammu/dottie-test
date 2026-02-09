package com.brimmatech.encompass.loanreader;

import com.brimmatech.encompass.loanreader.encompass.dto.Loan;
import com.brimmatech.encompass.loanreader.fieldReader.FieldReaderResponse;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.loanreader.pipeline.PipelineRequest;
import com.brimmatech.saas.PipelinePaginationRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public interface LoanReader {
    PipelinePaginationResponse[] fetchAllLoans(List<String> folderName,String accessToken);

    PipelinePaginationResponse[] fetchLoanGuid(String loanNumber,String encompassToken);

    List<PipelinePaginationResponse> fetchModifiedLoans(String accessToken, List<String> loanNumbers);

    PipelinePaginationResponse[] fetchMatchedLoan(String loanNumber, String accessToken);

    List<FieldReaderResponse> fetchLoanDetails(String loanGuid, List<String> fields, String accessToken);

    Loan fetchLoan(String loanGuid, String accessToken);

    Loan updateLoan(String loanGuid, JsonNode loanData, String accessToken);

    List<EncompassUserResponse> fetchUsers(String accessToken);

    List<PipelinePaginationResponse> fetchEligibleLoans(PipelinePaginationRequest pipelineRequest, String accessToken);

    PipelinePaginationResponse[] fetchApprovalLoan(PipelineRequest pipelineRequest, String accessToken);
}
