package com.brimmatech.princeton.dottie.conditions;

import com.brimmatech.docflow.common.email.AppEmailService;
import com.brimmatech.docflow.common.email.EmailRequest;
import com.brimmatech.docflow.common.email.MailServer;
import com.brimmatech.docflow.common.email.TenantMailServerResolver;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.fieldReader.FieldReaderResponse;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.mcp.LoanContextProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConditionalAutomateEmail {

    private final TemplateEngine templateEngine;
    private final AppEmailService appEmailService;
    private final TenantMailServerResolver tenantMailServerResolver;
    private final ConditionService conditionService;
    private final LoanReader loanReader;
    private static final List<String> ENHANCED_PRIOR_TO_ORDER = List.of("Approval", "Borrower CTC", "Collateral CTC", "Docs", "Closing", "Funding");
    private static final List<String> STANDARD_PRIOR_TO_ORDER = List.of("Approval", "Docs", "Closing", "Funding");
    private static final List<String> CATEGORY_ORDER = List.of("Assets", "Income", "Credit", "Property", "Title", "Misc");
    private final LoanContextProvider loanContextProvider;
    @Value("${princeton-condition-uri}")
    private String customConditionUiUrl;

    private String buildDottieUrl(String loanId, boolean enhanced) {
        String conditionType = enhanced ? "enhanced" : "standard";

        return customConditionUiUrl
                + loanId
                + "?conditionType="
                + conditionType;
    }


    public Map<String, List<ConditionDTO>> groupByPriorTo(
            List<ConditionDTO> conditions,
            boolean enhanced
    ) {

        List<String> priorToOrder =
                enhanced ? ENHANCED_PRIOR_TO_ORDER : STANDARD_PRIOR_TO_ORDER;

        return conditions.stream()
                .sorted(
                        Comparator
                                .comparingInt((ConditionDTO c) ->
                                        indexOf(priorToOrder, c.getPriorTo())
                                )
                                .thenComparingInt((ConditionDTO c) ->
                                        indexOf(CATEGORY_ORDER, c.getCategory())
                                )
                                .thenComparingInt(ConditionDTO::getOrder)
                )
                .collect(Collectors.groupingBy(
                        ConditionDTO::getPriorTo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }


    public void sendConditionalStatusEmail(String loanId, long tenantId, String flowName) throws Exception {

        TokenResponse
                tokenResponse = loanContextProvider.getToken(tenantId);

        List<FieldReaderResponse> loanResponse =
                loanReader.fetchLoanDetails(
                        loanId,
                        List.of("364", "4002", "2301", "2303", "984", "ENHANCEDCOND.X1", "2", "3", "762", "362",
                                "1409", "1408", "317", "1406", "1407", "LoanTeamMember.Email.LOA", "LoanTeamMember.Name.LOA", "LoanTeamMember.Phone.LOA" ),
                        tokenResponse.getAccessToken());

        Map<String, String> fields = loanResponse.stream()
                .filter(f -> f.getValue() != null)
                .collect(Collectors.toMap(
                        FieldReaderResponse::getFieldId,
                        f -> String.valueOf(f.getValue())
                ));

        String loanNumber = fields.get("364");
        String borrowerLastName = fields.get("4002");
        String approvalDate = fields.get("2301");
        String suspendedDate = fields.get("2303");
        String uwName = fields.get("984");
        String enhancedFlag = fields.get("ENHANCEDCOND.X1");

        boolean enhanced = "Y".equalsIgnoreCase(enhancedFlag);
        Boolean isConditionalApproval;

        String loanAmount          = fields.get("2");
        String interestRate        = fields.get("3");
        String rateLockDate        = fields.get("762");

        String processorName       = fields.get("362");
        String processorEmail      = fields.get("1409");
        String processorPhone      = fields.get("1408");

        String loFullName          = fields.get("317");
        String loEmail             = fields.get("1407");
        String loPhone             = fields.get("1406");

        String loaName             = fields.get("LoanTeamMember.Name.LOA");
        String loaEmail            = fields.get("LoanTeamMember.Email.LOA");
        String loaPhone            = fields.get("LoanTeamMember.Phone.LOA");

        if ("conditional-approval".equalsIgnoreCase(flowName)
                && StringUtils.hasText(approvalDate)) {
            isConditionalApproval = true;
        } else if ("suspended".equalsIgnoreCase(flowName)
                && StringUtils.hasText(suspendedDate)) {
            isConditionalApproval = false;
        } else {
            throw new IllegalStateException(
                    "Unable to determine loan status for flow=" + flowName
            );
        }

        List<JsonNode> Conditions =
                conditionService.fetchConditions(loanId, enhanced, tokenResponse.getAccessToken());

        List<ConditionDTO> allOpenConditions = conditionService.mapAndNormalize(Conditions, loanId, enhanced, tokenResponse.getAccessToken());

        List<ConditionDTO> filteredConditions =
                filterConditionsForEmail1(allOpenConditions, isConditionalApproval);

        Map<String, List<ConditionDTO>> groupedByPriorTo =
                groupByPriorTo(filteredConditions, enhanced);

        Context context = new Context();

        String loanStatus;
        String statusDate;

        if (isConditionalApproval) {
            context.setVariable("header", "Conditional Loan Approval");
            context.setVariable(
                    "introText",
                    "Congratulations! Your loan has been conditionally approved."
                            + "<br/><br/>"
                            + "Listed below are the conditions that must be satisfied before we can issue a Clear to Close."
            );
            loanStatus = "Conditionally Approved";
            statusDate = approvalDate;
        } else {
            context.setVariable("header", "Suspended");
            context.setVariable(
                    "introText",
                    "Your loan is currently suspended pending additional information."
                            + "<br/><br/>"
                            + "Listed below are the approval conditions that must be satisfied before we can issue a Conditional Approval."
            );
            loanStatus = "Suspended";
            statusDate = suspendedDate;
        }

        context.setVariable("dottieLink", buildDottieUrl(loanId, enhanced));
        context.setVariable("groupedConditions", groupedByPriorTo);
        context.setVariable("borrowerLastName", borrowerLastName);
        context.setVariable("loanNumber", loanNumber);
        context.setVariable("loanStatus", loanStatus);
        context.setVariable("loanStatusDate", statusDate);
        context.setVariable("underwriterName", uwName);
        context.setVariable("loanAmount", loanAmount);
        context.setVariable("interestRate", interestRate);
        context.setVariable("rateLockDate", rateLockDate);

        context.setVariable("processorName", processorName);
        context.setVariable("processorEmail", processorEmail);
        context.setVariable("processorPhone", processorPhone);

        context.setVariable("loFullName", loFullName);
        context.setVariable("loEmail", loEmail);
        context.setVariable("loPhone", loPhone);

        context.setVariable("loaName", loaName);
        context.setVariable("loaEmail", loaEmail);
        context.setVariable("loaPhone", loaPhone);


        String htmlContent = templateEngine.process(
                "princeton-dottie/conditional-status-email",
                context
        );
        EmailRequest request = new EmailRequest();
        request.setSubject(
                loanStatus + " - " + loanNumber + " " + borrowerLastName
        );
        request.setTextContent(htmlContent);

        request.setToList(List.of(loEmail, loaEmail));
        request.setCcList(List.of(processorEmail));

        MailServer tenantBasedServer =
                tenantMailServerResolver.resolveServer(tenantId);

        appEmailService.sendEmail(tenantBasedServer, request);
    }


    private List<ConditionDTO> filterConditionsForEmail1(
            List<ConditionDTO> allConditions,
            boolean isConditionalApproval
    ) {
        if (isConditionalApproval) {
            return allConditions;
        }

        return allConditions.stream()
                .filter(c ->
                        "Approval".equalsIgnoreCase(c.getPriorTo())
                                || "Borrower CTC".equalsIgnoreCase(c.getPriorTo())
                )
                .toList();
    }

    private int indexOf(List<String> order, String value) {
        if (value == null) return Integer.MAX_VALUE;
        int idx = order.indexOf(value);
        return idx == -1 ? Integer.MAX_VALUE : idx;
    }

    public void sendFileReviewCompleteEmail(
            String loanId,
            long tenantId,
            String flowName
    ) throws Exception {

        TokenResponse
                tokenResponse = loanContextProvider.getToken(tenantId);

        Map<String, String> fields =
                loanReader.fetchLoanDetails(
                        loanId,
                        List.of("364", "4002", "984", "ENHANCEDCOND.X1", "2", "3", "762", "362", "1409", "1408",
                                "317", "1406", "1407", "LoanTeamMember.Email.LOA", "LoanTeamMember.Name.LOA", "LoanTeamMember.Phone.LOA", "CX.COND.SUBMIT.COMPLETED"),
                        tokenResponse.getAccessToken()
                ).stream().collect(Collectors.toMap(
                        FieldReaderResponse::getFieldId,
                        v -> Objects.toString(v.getValue(), "")
                ));

        boolean isFileReviewed = "X".equalsIgnoreCase(fields.get("CX.COND.SUBMIT.COMPLETED"));

        if (isFileReviewed) {

            String loanNumber = fields.get("364");
            String borrowerLastName = fields.get("4002");
            String uwName = fields.get("984");

            boolean enhanced = "Y".equalsIgnoreCase(fields.get("ENHANCEDCOND.X1"));

            String loanAmount = fields.get("2");
            String interestRate = fields.get("3");
            String rateLockDate = fields.get("762");

            String processorName = fields.get("362");
            String processorEmail = fields.get("1409");
            String processorPhone = fields.get("1408");

            String loFullName = fields.get("317");
            String loEmail = fields.get("1407");
            String loPhone = fields.get("1406");

            String loaName = fields.get("LoanTeamMember.Name.LOA");
            String loaEmail = fields.get("LoanTeamMember.Email.LOA");
            String loaPhone = fields.get("LoanTeamMember.Phone.LOA");

            List<JsonNode> conditions =
                    conditionService.fetchConditions(loanId, enhanced, tokenResponse.getAccessToken());

            List<ConditionDTO> allOpenConditions = conditionService.mapAndNormalize(conditions, loanId, enhanced, tokenResponse.getAccessToken());

            List<ConditionDTO> approvalOpen =
                    allOpenConditions.stream()
                            .filter(c -> "Approval".equalsIgnoreCase(c.getPriorTo()))
                            .filter(ConditionDTO::isOpen)
                            .toList();

            List<ConditionDTO> borrowerOpen =
                    allOpenConditions.stream()
                            .filter(c -> "Borrower CTC".equalsIgnoreCase(c.getPriorTo()))
                            .filter(ConditionDTO::isOpen)
                            .toList();

            List<ConditionDTO> internalOpen =
                    allOpenConditions.stream()
                            .filter(c -> List.of(
                                    "Collateral CTC", "Docs", "Closing", "Funding"
                            ).contains(c.getPriorTo()))
                            .filter(ConditionDTO::isOpen)
                            .toList();

            List<ConditionDTO> rejected =
                    allOpenConditions.stream()
                            .filter(c -> "Rejected".equalsIgnoreCase(c.getStatus()))
                            .toList();

            long clearedCount = conditionService.countClearedOrWaivedConditions(conditions);

            Context context = new Context();
            context.setVariable("borrowerLastName", borrowerLastName);
            context.setVariable("loanNumber", loanNumber);
            context.setVariable("underwriterName", uwName);
            context.setVariable("reviewDate", LocalDate.now());

            context.setVariable("borrowerRemainingCount", borrowerOpen.size());
            context.setVariable("clearedCount", clearedCount);

            context.setVariable("approvalGroups", groupByPriorTo(approvalOpen, enhanced));
            context.setVariable("borrowerGroups", groupByPriorTo(borrowerOpen, enhanced));
            context.setVariable("internalGroups", groupByPriorTo(internalOpen, enhanced));
            context.setVariable("rejectedConditions", groupByPriorTo(rejected, enhanced));

            context.setVariable("showApproval", !approvalOpen.isEmpty());
            context.setVariable("showBorrower", !borrowerOpen.isEmpty());
            context.setVariable("showInternal", !internalOpen.isEmpty());
            context.setVariable("showRejected", !rejected.isEmpty());

            context.setVariable("dottieLink", buildDottieUrl(loanId, enhanced));

            context.setVariable("loanAmount", loanAmount);
            context.setVariable("interestRate", interestRate);
            context.setVariable("rateLockDate", rateLockDate);

            context.setVariable("processorName", processorName);
            context.setVariable("processorEmail", processorEmail);
            context.setVariable("processorPhone", processorPhone);

            context.setVariable("loFullName", loFullName);
            context.setVariable("loEmail", loEmail);
            context.setVariable("loPhone", loPhone);

            context.setVariable("loaName", loaName);
            context.setVariable("loaEmail", loaEmail);
            context.setVariable("loaPhone", loaPhone);

            String html =
                    templateEngine.process(
                            "princeton-dottie/file-review-complete-email",
                            context
                    );

            EmailRequest request = new EmailRequest();
            request.setSubject("Underwriter Review Complete - " + loanNumber + " " + borrowerLastName
            );
            request.setTextContent(html);
            request.setToList(List.of(loEmail, loaEmail));
            request.setCcList(List.of(processorEmail));

            appEmailService.sendEmail(
                    tenantMailServerResolver.resolveServer(tenantId),
                    request
            );
        }
    }

}
