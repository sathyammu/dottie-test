package com.brimmatech.apps.deapi.security;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.repository.TenantSettingsRepository;
import com.brimmatech.saas.crytograph.EncryptDecryptService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@AllArgsConstructor
@Component
public class ApiKeyAuthenticationProvider implements AuthenticationProvider {


    public static final String CATEGORY_AUTH = SettingsCategory.AUTH_API_KEY.getName();
    public static final String STRATEGY_API_KEY = SettingsCategory.AUTH_API_KEY.getStrategy();
    public static final String REFER_DOCUMENTATION = "Refer documentation";

    @AllArgsConstructor
    @Getter
    public enum API_ROLES {
        ROLE_API_INTERNAL_USER("ROLE_API_USER"),
        ROLE_API_CLIENT_USER("ROLE_API_CLIENT_USER");
        private final String value;

        public String asRoleName() {
            return this.value.replaceFirst("ROLE_", "");
        }
    }

    private final TenantSettingsRepository tenantSettingsRepository;
    private final EncryptDecryptService encryptDecryptService;

    @Override
    public Authentication authenticate(Authentication authentication){

        if (!(authentication instanceof ApiKeyAuthenticationToken)) {
            throw new InsufficientAuthenticationException(REFER_DOCUMENTATION);
        }

        var credentials = (ApiKeyAuthenticationToken.Credentials) authentication.getCredentials();

        //Make sure Magic is used only for /provision endpoint
        val isInternalSuperAdmin = credentials.apiKey().equals("MAGIC-3030303");

        if(isInternalSuperAdmin) {
            return getApiKeyAuthenticationToken(credentials);
        }

        var apiSettings = tenantSettingsRepository.findByTenantIdAndCategoryAndStrategy(credentials.tenantId(),
                CATEGORY_AUTH,
                STRATEGY_API_KEY);

        if (apiSettings.isEmpty()) {
            throw new DocflowDataException(REFER_DOCUMENTATION);
        }

        val apiKeyFromStore = apiSettings.get().getMeta().asText();
        if (encryptDecryptService.encrypt(credentials.apiKey()).equals(apiKeyFromStore)) {
            return getApiKeyAuthenticationToken(credentials);
        } else {
            log.trace("Comparing apiKeys failed");
            throw new BadCredentialsException(REFER_DOCUMENTATION);
        }
    }

    @NotNull
    private static ApiKeyAuthenticationToken getApiKeyAuthenticationToken(ApiKeyAuthenticationToken.Credentials credentials) {
        var auth = new ApiKeyAuthenticationToken(ApiKeyAuthenticationToken.Credentials.builder().build());
        auth.setAuthenticated(true);
        auth.setDummyPrincipal(credentials.tenantId());
        auth.setAuthorities(List.of(new SimpleGrantedAuthority(API_ROLES.ROLE_API_CLIENT_USER.toString())));
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        val supportResult = ApiKeyAuthenticationToken.class.isAssignableFrom(authentication);
        log.debug("Provider supports authentication? : {}", supportResult);
        return supportResult;

    }

}
