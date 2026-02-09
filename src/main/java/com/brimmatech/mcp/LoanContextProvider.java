package com.brimmatech.mcp;

import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.services.TenantSettingsService;
import com.brimmatech.encompass.loanreader.LoanReader;
import com.brimmatech.encompass.loanreader.pipeline.PipelinePaginationResponse;
import com.brimmatech.encompass.tokengenerator.TokenResponse;
import com.brimmatech.encompass.tokengenerator.TokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import static com.brimmatech.mcp.LoanCacheEntry.CacheField;

//TODO: Needs review. Remove/Clarify unused. Why is encompassBaseUrl not used.?
@Service @RequiredArgsConstructor @Slf4j public class LoanContextProvider {

    private final BeanHelper beanHelper;
    private final TenantEntityRepository tenantEntityRepository;
    private final TenantSettingsService tenantSettingsService;
    private final TokenService tokenService;
    private final WebClient encompassWebClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, LoanCacheEntry> loanCache = new ConcurrentHashMap<>();
    private final Map<Long, TokenCache> accessTokenCache = new ConcurrentHashMap<>();
    private final Map<Long, LoanReader> loanReaderCache = new ConcurrentHashMap<>();



    private <T> T getCachedValue(long tenantId, Map<Long, T> cache, Supplier<T> valueSupplier) {
        return cache.computeIfAbsent(tenantId, id -> valueSupplier.get());
    }

    public TokenResponse getToken(long tenantId){
       return getAccessToken(tenantId).getValue();
    }

    private Map.Entry<TenantEntity, TokenResponse> getAccessToken(long tenantId) {
        TenantEntity tenant = tenantEntityRepository.findById(tenantId)
                .orElseThrow(() -> new DocflowDataException("TenantId " + tenantId + " is not registered"));

        TokenCache
                tokenCache =
                accessTokenCache.computeIfAbsent(tenantId, id -> new TokenCache(tokenService.generateToken(tenant)));

        if (tokenCache.isExpired() || tokenCache.isInactive()) {
            log.info("Refreshing token for tenantId: {}", tenantId);
            TokenResponse newToken = tokenService.generateToken(tenant);
            tokenCache.updateToken(newToken);
        }

        return Map.entry(tenant, tokenCache.getTokenResponse());
    }

    private Map.Entry<LoanReader, TokenResponse> getLoanAndToken(long tenantId) {

        TokenResponse
                tokenResponse = getToken(tenantId);
        LoanReader
                loanReader =
                getCachedValue(tenantId,
                        loanReaderCache,
                        () -> beanHelper.getLoanReaderBean(tenantEntityRepository.findById(tenantId)
                                .orElseThrow(() -> new DocflowDataException(
                                        String.format("TenantId %d is not registered", tenantId)))
                                .getSor()
                                .getSystemOfRecordName() + "-loanreader"));

        return Map.entry(loanReader, tokenResponse);
    }


    private <T> T getOrUpdateCacheField(String loanNumber, CacheField field, Supplier<T> valueSupplier) {
        LoanCacheEntry cacheEntry = loanCache.compute(loanNumber, (key, existingEntry) -> {
            if (existingEntry == null || existingEntry.isExpired()) {
                return new LoanCacheEntry();
            }
            return existingEntry;
        });

        if (cacheEntry.getField(field) == null) {
            T value = valueSupplier.get();
            cacheEntry.updateField(field, value);
        }
        return (T) cacheEntry.getField(field);
    }

    private String getLoanId(String loanNumber, long tenantId) {
        return getOrUpdateCacheField(loanNumber, CacheField.LOAN_ID, () -> {
            val loanAndToken = getLoanAndToken(tenantId);
            TokenResponse tokenResponse = loanAndToken.getValue();
            LoanReader loanReader = loanAndToken.getKey();

            return Arrays.stream(loanReader.fetchMatchedLoan(loanNumber, tokenResponse.getAccessToken()))
                    .map(PipelinePaginationResponse::getLoanId)
                    .toList()
                    .get(0);
        });
    }
}
