package com.brimmatech.docflow.superadmin;

import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.superadmin.dto.PreferenceRequest;
import com.brimmatech.docflow.superadmin.dto.PreferenceResponse;
import com.brimmatech.docflow.v2.dto.TaskPayload;
import com.brimmatech.docflow.v2.task.TaskFactory;
import com.brimmatech.encompass.loanupdater.LOSLoanUpdateService;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.encompass.tokengenerator.TokenService;
import com.brimmatech.general.errorhandling.ValliaDataException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserFilterPreferenceService {

    private final UserFilterPreferenceRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenService tokenService;
    private final TaskFactory taskFactory;
    private final LOSLoanUpdateService losLoanUpdateService;
    private final TenantEntityRepository tenantEntityRepository;

    @Transactional
    public PreferenceResponse saveOrPatchPreference(PreferenceRequest request) throws Exception {
        String loEmail = request.getLoEmail();

        if (repository.existsByLoEmail(loEmail)) {
            // PATCH existing
            String patchJson = objectMapper.writeValueAsString(request.getPreferences());
            repository.patchPreferences(loEmail, patchJson);

            UserFilterPreference updated = repository.findByLoEmail(loEmail);

            return PreferenceResponse.builder()
                    .id(updated.getId())
                    .loEmail(updated.getLoEmail())
                    .preferences(updated.getUserPreferenceDetails())
                    .build();
        } else {
            // CREATE new
            UserFilterPreference entity = UserFilterPreference.builder()
                    .loEmail(loEmail)
                    .userPreferenceDetails(request.getPreferences())
                    .build();

            UserFilterPreference saved = repository.save(entity);

            return PreferenceResponse.builder()
                    .id(saved.getId())
                    .loEmail(saved.getLoEmail())
                    .preferences(saved.getUserPreferenceDetails())
                    .build();
        }
    }

    public PreferenceResponse getPreferenceByEmail(String loEmail) {

        UserFilterPreference entity = repository.findByLoEmail(loEmail);
        return PreferenceResponse.builder()
                .id(entity != null ? entity.getId() : null)
                .loEmail(loEmail)
                .preferences(
                        Optional.ofNullable(entity)
                                .map(UserFilterPreference::getUserPreferenceDetails)
                                .orElseGet(objectMapper::createArrayNode)
                )
                .build();
    }

    public String saveLoanCondition(
            String loanId,
            String conditionId,
            String conditionType,
            String userId,
            JsonNode comments
    ) {

        TenantEntity tenant = tenantEntityRepository.findByTenantNameIgnoreCase("PRINCETONMORTGAGE")
                .flatMap(list -> list.stream().findFirst())
                .orElseThrow(() -> new RuntimeException("Tenant not found for name PRINCETON MORTGAGE"));

        TokenResponse tokenResponse = tokenService.generateUserToken(tenant.getId(),userId);
        String message = "";
        String stepName = "UPDATE_CONDITION_COMMENTS";

        try {
            losLoanUpdateService.updateLoanFields(
                    loanId,
                    conditionId,
                    conditionType,
                    comments,
                    tokenResponse.getAccessToken()
            );

            message = "Loan fields updated successfully for loanId: " + loanId;

        } catch (WebClientResponseException e) {

            if (e.getStatusCode().equals(HttpStatus.CONFLICT)) {
                TaskPayload payload = TaskPayload.builder()
                        .loanId(loanId)
                        .conditionId(conditionId)
                        .conditionType(conditionType)
                        .comments(comments)
                        .userId(userId)
                        .timestamp(System.currentTimeMillis())
                        .build();
                taskFactory.initiateJobForUpdateEncompass(payload, loanId, tenant.getId(),stepName);
                message = "Loan locked — job initiated for Encompass update.";

            } else if (e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                throw new DocflowDataException(e.getResponseBodyAsString());

            } else {
                throw new ValliaDataException(e.getResponseBodyAsString());
            }
        }

        return message;
    }


}
