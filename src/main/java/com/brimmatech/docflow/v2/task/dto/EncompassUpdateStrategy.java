package com.brimmatech.docflow.v2.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor @Getter public enum EncompassUpdateStrategy {
    UPDATE_LOAN_NOTES("UPDATE_LOAN_NOTES", null),
    UPDATE_CONDITION_COMMENTS("UPDATE_CONDITION_COMMENTS", null),
    UPDATE_CONDITION_STATUS("UPDATE_CONDITION_STATUS", null),
    UPLOAD_ATTACHMENT("UPLOAD_ATTACHMENT", null),
    REMOVE_ATTACHMENT("REMOVE_ATTACHMENT", null),
    ASSIGN_DOCUMENTS("ASSIGN_DOCUMENTS", null);


    private final String value;
    private final Class<?> type;

    @Builder
    public record UPLOAD_SOURCE_FOR_EXTRACTED_DATA_MODEL(boolean shouldAccumulateAllRunsOnLoan, UploadEncompassFieldGroups fieldGroup){}

    public enum UploadEncompassFieldGroups {
        PURCHASE_ADVICE,
        CLASSIFY_UPLOADED_DOCS

    }
}
