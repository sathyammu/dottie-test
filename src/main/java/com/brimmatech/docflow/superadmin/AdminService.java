package com.brimmatech.docflow.superadmin;

import com.brimmatech.docflow.common.email.AppEmailService;
import com.brimmatech.docflow.common.email.EmailRequest;
import com.brimmatech.docflow.common.email.MailServer;
import com.brimmatech.docflow.common.email.TenantMailServerResolver;
import com.brimmatech.docflow.dfcommon.Tenant.TenantCredentials;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.encompass.loanreader.EncompassLoanReader;
import com.brimmatech.encompass.loanreader.EncompassUserResponse;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.encompass.tokengenerator.TokenService;
import com.brimmatech.encompass.tokengenerator.encompass.EncompassTokenGenerator;
import com.brimmatech.mcp.LoanContextProvider;
import com.brimmatech.saas.PipelinePaginationRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {
    private final TenantEntityRepository tenantEntityRepository;
    private final TokenService tokenService;
    private final LoanContextProvider loanContextProvider;
    private final ObjectMapper objectMapper;
    private final TemplateEngine templateEngine;
    private final AppEmailService appEmailService;
    private final EncompassLoanReader encompassLoanReader;
    private final EncompassTokenGenerator encompassTokenGenerator;
    private final TenantMailServerResolver tenantMailServerResolver;
    @Value("${princeton-custom-ui}")
    private String customUiUrl;

    @Value("${princeton-enable-prod-email}")
    private boolean sendEmailToLo;

    public String loanSummaryAttachmentEmail(long tenantId) {
        try {
            TenantEntity tenant = tenantEntityRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                log.error("Tenant not found for id {}", tenantId);
                return "Tenant not found!";
            }

            PipelinePaginationRequest pipelinePaginationRequest;
            try {
                pipelinePaginationRequest = objectMapper.readValue(
                        tenant.getPipelineRequest(),
                        PipelinePaginationRequest.class
                );
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse pipeline request JSON", e);
            }
            //TODO: LoanContextProvider
            TokenResponse adminTokenResponse = loanContextProvider.getToken(tenantId);
            String adminToken = adminTokenResponse.getAccessToken();
            TenantCredentials tenantCredentials = tokenService.getTenantCredentials(tenant);

            List<EncompassUserResponse> fetchUsers = encompassLoanReader.fetchUsers(adminToken);
            List<EncompassUserResponse> activeUsers = fetchUsers.stream()
                    .filter(u -> u.getUserIndicators() == null || !u.getUserIndicators().contains("Disabled"))
                    .filter(u -> u.getPersonas() != null && u.getPersonas().stream()
                            .anyMatch(p -> List.of("7", "1", "90", "11").contains(p.getEntityId())))
                    .toList();

            for (EncompassUserResponse activeUser : activeUsers) {
                String officerEmail = activeUser.getEmail();
                if (officerEmail == null || officerEmail.isBlank()) {
                    log.warn("Skipping user {} (no email)", activeUser.getFullName());
                    continue;
                }

                TokenResponse userTokenResponse = encompassTokenGenerator
                        .subjectImpersonationToken(adminToken, activeUser.getId(), tenantCredentials);

                List<PipelinePaginationResponse> loans = encompassLoanReader
                        .fetchEligibleLoans(pipelinePaginationRequest, userTokenResponse.getAccessToken());

                if (loans == null || loans.isEmpty()) {
                    log.info("No loans for officer {}", officerEmail);
                    continue;
                }

                List<LoanView> loanViews = new ArrayList<>();

                for (PipelinePaginationResponse loan : loans) {
                    //TODO: Use loanSummaryCache for saving summaries for reused loans
                    LoanView view = new LoanView();
                    view.setLoan(loan);

                    Map<String, String> subStatus = new HashMap<>();
                    Map<String, String> sectionStatus = new HashMap<>();

                    // ---------------- Disclosures ----------------
                    String discSent = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.DISCLOSURES"),
                            loan.getFields().get("Fields.3152"));
                    String itpRec = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.ITP"),
                            loan.getFields().get("Fields.3197"));
                    subStatus.put("DISCLOSURES_SENT", discSent);
                    subStatus.put("ITP_RECEIVED", itpRec);
                    sectionStatus.put("Disclosures", calculateMainStatus(Arrays.asList(discSent, itpRec)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("DISCLOSURES_SENT", "ITP_RECEIVED"));

                    // ---------------- Appraisal ----------------
                    String apprOrdered = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.APPRAISAL"),
                            loan.getFields().get("Fields.2352"));
                    String apprPaid = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.APPRAISAL.PAID"),
                            loan.getFields().get("Fields.CX.APPR.VA.INVPAID"));
                    String inspSched = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.APPRAISAL.SCHED"),
                            loan.getFields().get("Fields.CX.REG.APP.INSP.DT"));
                    String apprRec = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.APPRAISAL.RECD"),
                            loan.getFields().get("Fields.Document.DateReceived.Appraisal"));
                    String apprClear = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.APPRAISAL.CLEAR"),
                            loan.getFields().get("Fields.2353"));

                    subStatus.put("APPRAISAL_ORDERED", apprOrdered);
                    subStatus.put("APPRAISAL_PAID", apprPaid);
                    subStatus.put("APPRAISAL_SCHEDULED", inspSched);
                    subStatus.put("APPRAISAL_RECEIVED", apprRec);
                    subStatus.put("APPRAISAL_CLEARED", apprClear);
                    sectionStatus.put("Appraisal", calculateMainStatus(Arrays.asList(apprOrdered, apprPaid, inspSched, apprRec, apprClear)));

                    // PIW and VA logic
                    String piw = loan.getFields().get("Fields.CX.PIW");
                    String loanType = loan.getFields().get("Fields.1172");

                    if ("YES".equalsIgnoreCase(piw)) {
                        // PIW: mark received green and grey-out other appraisal items
                        subStatus.put("APPRAISAL_PIW_RECEIVED", "ld-complete-green"); // PIW subtitle
                        for (String s : List.of("APPRAISAL_ORDERED", "APPRAISAL_PAID", "APPRAISAL_SCHEDULED", "APPRAISAL_CLEARED", "APPRAISAL_RECEIVED")) {
                            subStatus.put(s, "ld-disabled");
                        }
                        loan.getFields().put("Fields.APPRAISAL_PIW_NOTE", "PIW");
                        sectionStatus.put("Appraisal", "ld-complete");
                    } else if ("VA".equalsIgnoreCase(loanType)) {
                        // VA loans: paid and scheduled are not applicable
                        subStatus.put("APPRAISAL_PAID", "ld-disabled");
                        subStatus.put("APPRAISAL_SCHEDULED", "ld-disabled");
                    }

                    // If appraisal received completed, disable previous steps
                    disablePreviousStepsIfLastCompleted(subStatus,
                            List.of("APPRAISAL_ORDERED", "APPRAISAL_PAID", "APPRAISAL_SCHEDULED", "APPRAISAL_RECEIVED", "APPRAISAL_CLEARED"));

                    // ---------------- Title ----------------
                    String titleOrd = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.TITLE.ORDER"),
                            loan.getFields().get("Fields.CUST21FV"));
                    String titleRec = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.TITLE.RECD"),
                            loan.getFields().get("Fields.CUST22FV"));
                    String titleClr = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.TITLE.CLEAR"),
                            loan.getFields().get("Fields.Condition.DateCleared.Property - Title Commitment"));
                    subStatus.put("TITLE_ORDERED", titleOrd);
                    subStatus.put("TITLE_RECEIVED", titleRec);
                    subStatus.put("TITLE_CLEARED", titleClr);
                    sectionStatus.put("Title", calculateMainStatus(Arrays.asList(titleOrd, titleRec, titleClr)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("TITLE_ORDERED", "TITLE_RECEIVED", "TITLE_CLEARED"));

                    // ---------------- HOI ----------------
                    // (Fixed typos and corrected field keys)
                    String hoiRec = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.HOI.RECD"),
                            loan.getFields().get("Fields.Document.DateReceived.Property - Homeowners Insurance Policy"));
                    String hoiClr = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.HOI.CLEAR"),
                            loan.getFields().get("Fields.Condition.DateCleared.Property - Homeowners Insurance Policy"));
                    subStatus.put("HOI_RECEIVED", hoiRec);
                    subStatus.put("HOI_CLEARED", hoiClr);
                    sectionStatus.put("HOI", calculateMainStatus(Arrays.asList(hoiRec, hoiClr)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("HOI_RECEIVED", "HOI_CLEARED"));

                    // ---------------- Init. Approval ----------------
                    String initSub = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.SUB.UW"),
                            loan.getFields().get("Fields.Log.MS.Date.Submit to UW"));
                    String initRec = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.COND.APP"),
                            loan.getFields().get("Fields.2301"));
                    subStatus.put("INIT_SUBMIT", initSub);
                    subStatus.put("INIT_APPROVAL", initRec);
                    sectionStatus.put("InitApproval", calculateMainStatus(Arrays.asList(initSub, initRec)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("INIT_SUBMIT", "INIT_APPROVAL"));

                    // ---------------- Locked ----------------
                    String locked = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.LOCK"),
                            loan.getFields().get("Fields.761"));
                    subStatus.put("LOCKED", locked);
                    sectionStatus.put("Locked", locked);

                    // ---------------- Borrower CTC ----------------
                    String borrSub = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.BORR.CONDITION"),
                            loan.getFields().get("Fields.CX.BCSUBMIT.ACTUAL"));
                    String borrClr = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.BORR.COND.CLEAR"),
                            loan.getFields().get("Fields.CX.COND.BORR.CLEAR.DATE"));
                    subStatus.put("BORR_SUBMIT", borrSub);
                    subStatus.put("BORR_CLEAR", borrClr);
                    sectionStatus.put("BorrCTC", calculateMainStatus(Arrays.asList(borrSub, borrClr)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("BORR_SUBMIT", "BORR_CLEAR"));

                    // ---------------- Collateral CTC ----------------
                    String collSub = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.COLL.CONDITION"),
                            loan.getFields().get("Fields.CX.PCSUBMIT.ACTUAL"));
                    String collClr = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.COLL.COND.CLEAR"),
                            loan.getFields().get("Fields.CX.COND.PROP.CLEAR.DATE"));
                    subStatus.put("COLL_SUBMIT", collSub);
                    subStatus.put("COLL_CLEAR", collClr);
                    sectionStatus.put("CollCTC", calculateMainStatus(Arrays.asList(collSub, collClr)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("COLL_SUBMIT", "COLL_CLEAR"));

                    // ---------------- Final CTC ----------------
                    String finalSub = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.CTC.SUBMIT"),
                            loan.getFields().get("Fields.CX.FINAL.SUBMIT.ACTUAL"));
                    String finalClr = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.CTC.CLEAR"),
                            loan.getFields().get("Fields.2305"));
                    subStatus.put("FINAL_SUBMIT", finalSub);
                    subStatus.put("FINAL_CLEAR", finalClr);
                    sectionStatus.put("FinalCTC", calculateMainStatus(Arrays.asList(finalSub, finalClr)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("FINAL_SUBMIT", "FINAL_CLEAR"));

                    // ---------------- Init. CD ----------------
                    String icdReq = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.ICD.REQ"),
                            loan.getFields().get("Fields.CX.EARLYCD.DATE"));
                    String icdSent = calculateSubStatus(loan.getFields().get("Fields.CX.CDSEND.DEADLINE"),
                            loan.getFields().get("Fields.3977"));
                    subStatus.put("ICD_REQUEST", icdReq);
                    subStatus.put("ICD_SENT", icdSent);
                    sectionStatus.put("InitCD", calculateMainStatus(Arrays.asList(icdReq, icdSent)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("ICD_REQUEST", "ICD_SENT"));

                    // ---------------- Final Verbal ----------------
                    String finalVerb = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.VVOE"),
                            loan.getFields().get("Fields.CX.VVOE.RECD.DT"));
                    subStatus.put("FINAL_VERBAL", finalVerb);
                    sectionStatus.put("FinalVerbal", finalVerb);

                    // ---------------- Closing (renamed, add Funds Sent) ----------------
                    String closeSub = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.2.CLOSING"),
                            loan.getFields().get("Fields.Log.MS.Date.Ready for Docs"));
                    String closeSent = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.DOCSENT"),
                            loan.getFields().get("Fields.Log.MS.Date.Docs Signing"));
                    String closeFunds = calculateSubStatus(loan.getFields().get("Fields.CX.DUE.DOCSENT"),
                            loan.getFields().get("Fields.1997"));
                    subStatus.put("CLOSING_SUBMIT", closeSub);
                    subStatus.put("CLOSING_SENT", closeSent);
                    subStatus.put("CLOSING_FUNDS_SENT", closeFunds);
                    sectionStatus.put("Closing", calculateMainStatus(Arrays.asList(closeSub, closeSent, closeFunds)));
                    disablePreviousStepsIfLastCompleted(subStatus, List.of("CLOSING_SUBMIT", "CLOSING_SENT", "CLOSING_FUNDS_SENT"));

                    // ---------------- Add to view ----------------
                    view.setSubStatus(subStatus);
                    view.setSectionStatus(sectionStatus);

                    long pastDue = subStatus.values().stream().filter(status -> "ld-past".equalsIgnoreCase(status)).count();
                    view.setPastDueCount((int) pastDue);

                    int deadInWater = calculateDeadInWater(loan);
                    view.getLoan().getFields().put("Fields.DEAD_IN_THE_WATER", String.valueOf(deadInWater));

                    loanViews.add(view);
                }

                loanViews.sort(Comparator.comparingInt(LoanView::getPastDueCount).reversed());

                Context context = new Context();
                context.setVariable("dashboardUrl", customUiUrl);
                context.setVariable("loans", loanViews);

                String htmlContent = templateEngine.process("loan-summary", context);

                //TODO: Don't user byte[]
                byte[] pdfBytes;
                try (ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {
                    PdfRendererBuilder builder = new PdfRendererBuilder();
                    builder.useFastMode();
                    String baseUri = this.getClass().getResource("/").toString();
                    builder.withHtmlContent(htmlContent, baseUri);
                    builder.toStream(pdfStream);
                    builder.run();
                    pdfBytes = pdfStream.toByteArray();
                }

                File tempFile = File.createTempFile("loan-summary-" + LocalDate.now().format(DateTimeFormatter.ISO_DATE), ".pdf");
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(pdfBytes);
                }

                String emailBody = generateLoanSummaryEmailBody(loans);
                String subject = "Loan Pipeline Summary - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

                EmailRequest request = new EmailRequest();
                request.setSubject(subject);
                request.setTextContent(emailBody);
                request.setAttachments(Collections.singletonList(tempFile));
                if (sendEmailToLo) {
                     request.setToList(List.of(activeUser.getEmail()));
                } else {
                    request.setToList(List.of("sathya.selvi@brimmatech.com"));
                }

                MailServer tenantBasedServer = tenantMailServerResolver.resolveServer(tenantId);
                appEmailService.sendEmail(tenantBasedServer, request);
                log.info("Email sent to {} with {} loans", activeUser.getEmail(), loans.size());
            }

            return "Emails Sent with PDF attachments!";
        } catch (Exception e) {
            log.error("Unable to send loan summary attachment emails", e);
            return "Failed to send emails";
        }
    }

    // ---------------- Utility helpers ----------------
    private void disablePreviousStepsIfLastCompleted(Map<String, String> subStatus, List<String> stepOrder) {
        // iterate backwards: if a later step is complete, disable earlier steps that are past/due
        for (int i = stepOrder.size() - 1; i >= 0; i--) {
            String step = stepOrder.get(i);
            String val = subStatus.get(step);
            if (val != null && (val.equals("ld-complete"))) {
                for (int j = 0; j < i; j++) {
                    String prev = stepOrder.get(j);
                    String prevVal = subStatus.get(prev);
                    if (prevVal != null && (prevVal.equals("ld-past") || prevVal.equals("ld-due") || prevVal.equals("ld-progress"))) {
                        subStatus.put(prev, "ld-disabled");
                    }
                }
            }
        }
    }

    private String generateLoanSummaryEmailBody(List<PipelinePaginationResponse> loans) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Loan Pipeline Summary</h2>");
        sb.append("<p>There are ").append(loans.size()).append(" loans with due/past due conditions.</p>");
        sb.append("<ul style='font-family: Arial, sans-serif; font-size: 14px; padding-left: 20px;'>");
        for (PipelinePaginationResponse loan : loans) {
            String rawLoanNumber = loan.getFields().get("Loan.LoanNumber");
            String loanNumber = (rawLoanNumber != null && rawLoanNumber.length() > 5)
                    ? rawLoanNumber.substring(rawLoanNumber.length() - 5)
                    : (rawLoanNumber != null ? rawLoanNumber : "N/A");
            String borrowerFullName = loan.getFields().get("Fields.4002");
            String borrowerFirstName = (borrowerFullName != null && borrowerFullName.contains(" "))
                    ? borrowerFullName.substring(0, borrowerFullName.indexOf(" "))
                    : (borrowerFullName != null ? borrowerFullName : "N/A");
            String currentMilestone = loan.getFields().get("Loan.CurrentMilestoneName");
            List<String> pastDue = getPastDueConditions(loan);
            String pastDueText = pastDue.isEmpty() ? "No past due conditions." : "Loan " + loanNumber + " has: " + String.join(", ", pastDue) + ".";
            sb.append("<li>").append(loanNumber).append(" [").append(borrowerFirstName).append("] - ")
                    .append(currentMilestone).append(" - ")
                    .append("<a href='").append(customUiUrl).append("'>See in Dashboard &#8599;</a>")
                    .append("<div>").append(pastDueText).append("</div></li>");
        }
        sb.append("</ul>");
        return sb.toString();
    }

    private List<String> getPastDueConditions(PipelinePaginationResponse loan) {
        List<String> conditions = new ArrayList<>();
        Map<String, String> fields = loan.getFields();

        if (isPastDue(fields.get("Fields.CX.DUE.BORR.CONDITION"), fields.get("Fields.CX.BCSUBMIT.ACTUAL"))) {
            conditions.add("Borrower CTC Submitted");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.BORR.COND.CLEAR"), fields.get("Fields.CX.COND.BORR.CLEAR.DATE"))) {
            conditions.add("Borrower CTC Approved");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.COLL.CONDITION"), fields.get("Fields.CX.PCSUBMIT.ACTUAL"))) {
            conditions.add("Collateral CTC Submitted");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.COLL.COND.CLEAR"), fields.get("Fields.CX.COND.PROP.CLEAR.DATE"))) {
            conditions.add("Collateral CTC Approved");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.ICD.REQ"), fields.get("Fields.CX.EARLYCD.DATE"))) {
            conditions.add("Initial CD Requested");
        }
        if (isPastDue(fields.get("Fields.CX.CDSEND.DEADLINE"), fields.get("Fields.3977"))) {
            conditions.add("Initial CD Sent");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.2.CLOSING"), fields.get("Fields.Log.MS.Date.Ready for Docs"))) {
            conditions.add("Closing Docs Submitted");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.DOCSENT"), fields.get("Fields.Log.MS.Date.Docs Signing"))) {
            conditions.add("Closing Docs Sent");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.HOI.RECD"), fields.get("Fields.Document.DateReceived.Property - Homeowner's Insurance Policy"))) {
            conditions.add("HOI Received");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.HOI.CLEAR"), fields.get("Fields.Condition.DateCleared.Property - Homeowners Insurance Policy"))) {
            conditions.add("HOI Cleared");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.APPRAISAL.PAID"), fields.get("Fields.CX.APPR.VA.INVPAID"))) {
            conditions.add("Appraisal Paid");
        }
        if (isPastDue(fields.get("Fields.CX.DUE.APPRAISAL.CLEAR"), fields.get("Fields.2353"))) {
            conditions.add("Appraisal Cleared");
        }
        return conditions;
    }

    private boolean isPastDue(Object dueDateObj, Object completedDateObj) {
        if (dueDateObj == null || dueDateObj.toString().isBlank()) return false;
        LocalDate dueDate = parseDate(dueDateObj.toString());
        if (dueDate == null) return false;
        LocalDate today = LocalDate.now();
        if (completedDateObj != null && !completedDateObj.toString().isBlank()) {
            LocalDate completed = parseDate(completedDateObj.toString());
            if (completed == null) return false;
            return completed.isAfter(dueDate);
        }
        return today.isAfter(dueDate);
    }

    public String calculateSubStatus(String dueDateStr, String completedDateStr) {
        boolean hasDue = dueDateStr != null && !dueDateStr.isBlank();
        boolean hasCompleted = completedDateStr != null && !completedDateStr.isBlank();

        LocalDate dueDate = hasDue ? parseDate(dueDateStr) : null;
        LocalDate completedDate = hasCompleted ? parseDate(completedDateStr) : null;
        LocalDate today = LocalDate.now();

        if (completedDate != null) {
            return "ld-complete";
        }

        if (dueDate != null) {
            if (dueDate.isBefore(today)) return "ld-past";
            if (dueDate.isEqual(today)) return "ld-due";
        }

        return "ld-not";
    }

    public String calculateMainStatus(List<String> subStatuses) {
        if (subStatuses.stream().allMatch(s -> "ld-not".equals(s))) return "ld-not";
        if (subStatuses.stream().anyMatch(s -> "ld-past".equals(s))) return "ld-past";
        if (subStatuses.stream().anyMatch(s -> "ld-due".equals(s))) return "ld-due";
        if (subStatuses.stream().allMatch(s -> s.startsWith("ld-complete"))) return "ld-complete";
        return "ld-progress";
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isEmpty()) return null;
        List<String> patterns = Arrays.asList(
                "yyyy-MM-dd",
                "MM/dd/yyyy",
                "M/d/yyyy",
                "MM/dd/yyyy hh:mm:ss a",
                "M/d/yyyy h:mm:ss a",
                "M/d/yyyy hh:mm:ss a"
        );
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH);
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    public int calculateDeadInWater(PipelinePaginationResponse loan) {
        String itpRaw = loan.getFields().get("Fields.3197");
        LocalDate itpDate = parseDate(itpRaw);
        if (itpDate == null) return 0;
        String fileReviewRaw = loan.getFields().get("Fields.Log.MS.Date.File Review-Retl");
        LocalDate comparisonDate = StringUtils.hasText(fileReviewRaw) ? parseDate(fileReviewRaw) : LocalDate.now();
        if (comparisonDate == null) comparisonDate = LocalDate.now();
        int days = (int) ChronoUnit.DAYS.between(itpDate, comparisonDate);
        return Math.max(days, 0);
    }
}