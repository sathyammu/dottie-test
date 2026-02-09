package com.brimmatech.encompass.tokengenerator;

import com.brimmatech.azure.keyvault.KeyVaultService;
import com.brimmatech.docflow.common.beanhelper.BeanHelper;
import com.brimmatech.docflow.dfcommon.Tenant.TenantCredentials;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntity;
import com.brimmatech.docflow.dfcommon.Tenant.TenantEntityRepository;
import com.brimmatech.encompass.tokengenerator.encompass.EncompassTokenGenerator;
import com.brimmatech.saas.client.Client;
import com.brimmatech.saas.client.ClientRepository;
import com.brimmatech.saas.crytograph.EncryptDecryptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

    public static final String DOCFLOW_ENCOMPASS_CLIENT_ID = "docflow-encompass-client-id-";
    public static final String DOCFLOW_ENCOMPASS_CLIENT_SECRET = "docflow-encompass-client-secret-";
    public static final String DOCFLOW_ENCOMPASS_USERNAME = "docflow-encompass-username-";
    public static final String DOCFLOW_ENCOMPASS_PASSWORD = "docflow-encompass-password-";
    public static final String DOCFLOW_ENCOMPASS_INSTANCE_ID = "docflow-encompass-instance-id-";
    public static final String SHAREPOINT_URL = "sharepoint-url-";
    public static final String SHAREPOINT_FOLDER_NAME = "sharepoint-folder-name-";
    public static final String GRAPH_CLIENT_ID = "graph-client-id-";
    public static final String GRAPH_CLIENT_SECRET = "graph-client-secret-";
    public static final String GRAPH_TENANT_ID = "graph-tenant-id-";
    public static final String SITE_NAME = "site-name-";
    public static final String SFTP = "docflow-sftp-";

    public static Map<String, String> secrets = new HashMap<>();
    private final ObjectMapper objectMapper;
    private final BeanHelper beanHelper;
    private final ClientRepository clientRepository;
    private final KeyVaultService keyVaultService;
    private final TenantEntityRepository tenantEntityRepository;
    private final EncompassTokenGenerator encompassTokenGenerator;
    private final EncryptDecryptService encryptDecryptService;
    @Value("${tenant.credential.profile}")
    private String tenantCredentialProfile;


    @Deprecated
    /**
     * Use LoanContextProvider
     */
    public TokenResponse generateToken(TenantEntity tenant) {

        log.info("Generating token for tenant :{}", tenant.getId());

        TenantCredentials tenantCredentials = getTenantCredentials(tenant);

        TokenGenerator tokenGenerator = beanHelper.getTokenGenerator(tenant.getSor().getSystemOfRecordName());

        TokenResponse tokenResponse = null;

        if (!StringUtils.hasText(tenantCredentials.getUserName()) &&
                !StringUtils.hasText(tenantCredentials.getPassword())) {
            tokenResponse = tokenGenerator.clientTokenGenerator(tenantCredentials);
        } else {
            tokenResponse = tokenGenerator.defaultTokenGenerator(tenantCredentials);
        }

        return tokenResponse;
    }

    public TenantCredentials getTenantCredentials(TenantEntity tenant) {
        TenantCredentials tenantCredentials;

        if ("local".equalsIgnoreCase(tenantCredentialProfile)) {
            tenantCredentials = mapCredentials(tenant);
        } else {
            tenantCredentials = getCredentialsFromKeyVault(tenant);
        }
        return tenantCredentials;
    }

    private TenantCredentials mapCredentials(TenantEntity tenantEntity) {

        log.info("Mapping tenant metadata into tenant credentials for tenantId :{}", tenantEntity.getId());

        TenantCredentials tenantCredentials = new TenantCredentials();
        try {
            tenantCredentials = objectMapper.readValue(tenantEntity.getSorMetadata(), TenantCredentials.class);
        } catch (JsonProcessingException e) {
            log.info("Couldn't able to parse the metadata into tenantCredentials for tenant :{}",
                    tenantEntity.getTenantName());
        }
        return tenantCredentials;
    }


    public TenantCredentials getCredentialsFromKeyVault(TenantEntity tenantEntity) {

        Client client = getClientByTenantName(tenantEntity.getTenantName());
        String clientId = client.getClientId().toString();

        log.info("Getting credentials from the key vault for the client id: {}", clientId);

        List<String> requiredSecrets = List.of(
                DOCFLOW_ENCOMPASS_CLIENT_ID + clientId,
                DOCFLOW_ENCOMPASS_CLIENT_SECRET + clientId,
                DOCFLOW_ENCOMPASS_USERNAME + clientId,
                DOCFLOW_ENCOMPASS_PASSWORD + clientId,
                DOCFLOW_ENCOMPASS_INSTANCE_ID + clientId
        );

        List<String> optionalSecrets = List.of(
                GRAPH_CLIENT_ID + clientId,
                GRAPH_CLIENT_SECRET + clientId,
                GRAPH_TENANT_ID + clientId,
                SHAREPOINT_URL + clientId,
                SHAREPOINT_FOLDER_NAME + clientId,
                SITE_NAME + clientId
        );

        List<String> allSecrets = new ArrayList<>();
        allSecrets.addAll(requiredSecrets);
//        allSecrets.addAll(optionalSecrets);

        List<String> missingSecrets = allSecrets.stream()
                .filter(secret -> !secrets.containsKey(secret))
                .toList();

        if (!missingSecrets.isEmpty()) {
            log.debug("Fetching missing secrets from Key Vault: {}", missingSecrets);

            Map<String, String> fetchedSecrets = keyVaultService.getSecrets(missingSecrets);
            for (Map.Entry<String, String> entry : fetchedSecrets.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (requiredSecrets.contains(key) && !StringUtils.hasText(value)) {
                    throw new RuntimeException("Missing or empty value for required secret: " + key);
                }

                if (StringUtils.hasText(value)) {
                    secrets.put(key, value);
                }
            }
        }

        Map<String, String> credentials = allSecrets.stream()
                .filter(secrets::containsKey)
                .collect(Collectors.toMap(secretName -> secretName, secrets::get));

        log.info("Successfully retrieved secrets for client id: {}", clientId);

        return buildTenantCredentials(credentials, clientId);
    }

    private Client getClientByTenantName(String tenantName) {
        log.info("Checking whether the tenant name {} matches the client name", tenantName);

        Client client = clientRepository.findByClientNameIgnoreCase(tenantName);

        if (client == null) {
            log.warn("No client found with name: {}", tenantName);
            throw new RuntimeException("Client not found for tenant name: " + tenantName);
        }

        log.info("Client name {} matches tenant name: {}", client.getClientName(), tenantName);
        return client;
    }

    private TenantCredentials buildTenantCredentials(Map<String, String> credentials, String clientId) {
        log.info("Building TenantCredentials for clientId: {}", clientId);

        TenantCredentials tenantCredentials = new TenantCredentials();

        tenantCredentials.setClientId(getOrThrow(credentials, DOCFLOW_ENCOMPASS_CLIENT_ID + clientId));
        tenantCredentials.setClientSecret(getOrThrow(credentials, DOCFLOW_ENCOMPASS_CLIENT_SECRET + clientId));
        tenantCredentials.setUserName(getOrThrow(credentials, DOCFLOW_ENCOMPASS_USERNAME + clientId));
        tenantCredentials.setPassword(getOrThrow(credentials, DOCFLOW_ENCOMPASS_PASSWORD + clientId));
        tenantCredentials.setInstanceId(getOrThrow(credentials, DOCFLOW_ENCOMPASS_INSTANCE_ID + clientId));
        log.info("Successfully built TenantCredentials for clientId: {}", clientId);

        return tenantCredentials;
    }

    private String getOrThrow(Map<String, String> credentials, String key) {
        String value = credentials.get(key);
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException("Missing or empty value for key: " + key);
        }
        return value;
    }


    public TokenResponse generateUserToken(Long tenantId, String userId) {
        return tenantEntityRepository.findById(tenantId)
                .map(tenant -> {
                    String adminToken = generateToken(tenant).getAccessToken();
                    TenantCredentials credentials = getTenantCredentials(tenant);
                    return encompassTokenGenerator.subjectImpersonationToken(adminToken, userId, credentials);
                })
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found for ID: " + tenantId));
    }

}