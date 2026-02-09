package com.brimmatech.encompass.tokengenerator;

import com.brimmatech.docflow.dfcommon.Tenant.TenantCredentials;

public interface TokenGenerator {
    TokenResponse defaultTokenGenerator(TenantCredentials tenant);
    TokenResponse clientTokenGenerator(TenantCredentials tenant);
    TokenResponse subjectImpersonationToken(String accessToken,String userId,TenantCredentials tenantCredentials);
}
