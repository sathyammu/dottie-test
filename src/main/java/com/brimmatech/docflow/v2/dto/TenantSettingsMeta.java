package com.brimmatech.docflow.v2.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import kotlin.jvm.Transient;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class TenantSettingsMeta {
    @JsonProperty("eventType")
    private TaskTriggeringEventType eventType;

    @JsonProperty("allowedList")
    private List<String> allowedList;

    @JsonProperty("allowedFolderList")
    private List<String> allowedFolderList;

    @JsonProperty("allowedDocumentList")
    private List<String> allowedDocumentList;

    @JsonProperty("allowedDocType")
    private List<String> allowedDocType;

    @JsonProperty("auth_key")
    private String authKey;

    @JsonProperty("action")
    private TenantSettingsMetaAction action;

    @JsonProperty("shouldNotCreateRuleBatch")
    private boolean shouldNotCreateRuleBatch;

    @JsonProperty("flowName")
    private TaskBusinessFlowName flowName;

    @JsonProperty("flowDescription")
    private String flowDescription;

    @JsonProperty("uploadDocument")
    private boolean uploadDocument;

    @JsonProperty("uploadDocumentInSharePoint")
    private boolean uploadDocumentInSharePoint;

    @JsonProperty("manualTrigger")
    private boolean manualTrigger;

    @JsonProperty("triggerValue")
    private String triggerValue;

    @JsonIgnore
    @Transient
    private boolean isBailAfterSubstitutions;

    @JsonIgnore
    @Transient
    private JsonNode dynamicallyModifiedRoute;


    public enum RouteEntryPointType {
        RULE_EXECUTIONS,
        DOC_UPLOAD
    }

    public enum TriggerValue {
        sFundD,
        custLoan475
    }

    public enum TaskTriggeringEventType {
        document_classification,
        split_zip_bayequity_closing_package,
        DISCLOSURE_FIELD_CHANGE,
        invoke_api,
        purchase_advice_via_llm,
        purchase_advice,
        document_extraction,
        purchase_condition,
        milestone,
        fieldchange,
        document,
        extract_via_llm,
        classify_via_llm,
        task_via_routing,
        move,
        attachment
    }

    @RequiredArgsConstructor
    public enum TaskBusinessFlowName {

        purchase_advice("purchase-advice", "Purchase advice workflow"),
        ctc("ctc", "Clear to close workflow"),
        purchase_condition("purchase-condition", "Purchase condition processing"),
        document_classification("document_classification", "Uploaded document classification"),
        signed_disclosure("signed_disclosure", "Signed disclosure processing"),
        ask_and_analyze("ask-and-analyze", "Ask and analyze flow"),
        amc("amc", "AMC document upload and processing"),
        cornerstone("cornerstone", "Cornerstone document upload and processing"),
        approval_disclosure("approval-disclosure", "Approval disclosure workflow"),
        pre_closing_disclosure("pre-closing-disclosure", "Preclosing disclosure workflow"),
        dottie_loan_notes("dottie-loan-notes", "Dottie loan notes processing"),
        split_zip_closing_package("split_zip_closing_package", "Split ZIP closing package"),
        purchase_advice_llm("purchase-advice-llm", "Purchase advice LLM flow"),
        temple_view("temple-view", "Temple view integration"),
        tml_rules("tml-rules", "TML rules execution"),
        tml_rules_s2("tml-rules-s2", "TML rules stage 2"),
        pre_underwriting_validator("pre-underwriting-validator", "Pre underwriting validation"),
        credit_rules("credit-rules", "Credit rules execution"),
        INVOKE_API("invoke_api", "Invoke API"),
    PRINCETON_INDEXING_FLOW("PRINCETON_INDEXING_FLOW", "Update custom fields from a single document"),
        closing_disclosure_validation(
                "closing-disclosure-validation",
                "Closing disclosure validation"
        ),
        oak_tree_custom_extraction("oak_tree_custom_extraction","custom extraction"),
        qc_checklist("qc_checklist", "Quality control checklist processing"),
        loan_migrate("loan_migrate","Loan Migrate"),
        all_title_rules("all-title-rules","Title - Rules"),
    intent_to_proceed("intent_to_proceed","Intent To Proceed"),
        coc_disclosure("coc-disclosure", "Change Of Circumstance");


        private final String flowName;
        private final String flowDescription;

        @JsonCreator
        public static TaskBusinessFlowName fromValue(String value) {
            if (value == null) return null;

            String normalized = value.trim()
                    .toLowerCase()
                    .replace("-", "_");

            return Arrays.stream(values())
                    .filter(v ->
                            v.flowName.equalsIgnoreCase(normalized)
                                    || v.name().equalsIgnoreCase(normalized)
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Unknown flowName: " + value));
        }

        @JsonValue
        public String getFlowName() {
            return flowName;
        }
        @JsonIgnore
        public String getFlowDescription() {
            return flowDescription;
        }
    }
}
