# Configuring Client Credentials (CC) Flow for New Tenant

Here’s an expanded explanation of the steps and hints for implementing auth over client credentials (CC) flow with the required settings in `tenant_settings`.

## 1. Create Two Settings in `tenant_settings`

You need to add two settings in the `tenant_settings` table:

- **`NOTIFICATION_AUTH`**: Contains the configuration for the client credentials (CC) flow.
- **`NOTIFICATION_SIGNER_KEYNAME`**: Specifies the name of the Key Vault key used for signing tokens.

### `NOTIFICATION_AUTH` Structure

The `NOTIFICATION_AUTH` setting should have the following JSON structure:

```json
{
  "clientId": "<client provided client-id>",
  "tokenEndpoint": ".../oauth/token",
  "assertionAudience": "url ending with slash/",
  "resourceServer": "<resource server>",
  "strategy": "cc",
  "tokenExpirySeconds": 30
}
```

#### Explanation of Fields:
- **`clientId`**: The client ID provided by the client.
- **`tokenEndpoint`**: The URL for obtaining tokens (OAuth token endpoint).
- **`assertionAudience`**: The audience URL, which must end with a `/`.
- **`resourceServer`**: The resource server identifier.
- **`strategy`**: Set to `"cc"` for client credentials flow.
- **`tokenExpirySeconds`**: Token expiration time in seconds (e.g., 30 seconds).

---

## 2. `NOTIFICATION_SIGNER_KEYNAME`

This setting specifies the name of the Key Vault key used for signing tokens. The key can be created using Azure CLI.

### Command to Create the Key:
```bash
az deployment group create \
  --resource-group $RG \
  --template-file provision-keys.bicep \
  --parameters principalObjectId=$OBJECT_ID \
  --parameters keyVaultName=kv-brimma-dev \
  --parameters keyName=jwt-signing-key-wilqo-prod
```

#### Parameters:
- **`$RG`**: Resource group name.
- **`$OBJECT_ID`**: Object ID of the principal (e.g., service principal or user).
- **`keyVaultName`**: Name of the Key Vault (e.g., `kv-brimma-dev`).
- **`keyName`**: Name of the key (e.g., `jwt-signing-key-wilqo-prod`).

---

## 3. Download the Certificate

Once the key is created, download the certificate using the following API:

```http
POST {{ORCHESTRATOR_URL}}/admin/hooks/self-sign-token/cert-as-public-key/<tenantId>
```

### Steps:
1. Replace `{{ORCHESTRATOR_URL}}` with the orchestrator's base URL.
2. Replace `<tenantId>` with the tenant's ID.
3. Format the downloaded certificate properly (e.g., PEM format).
4. Send the formatted certificate to the client.

---

## 4. Client Provides Required Information

The client will share the following details:
- **`clientId`**: Client ID.
- **`tokenEndpoint`**: OAuth token endpoint URL.
- **`resourceServer`**: Resource server identifier.

Add these details to the `NOTIFICATION_AUTH` setting in the `tenant_settings` table.

---

## 5. Configure Hook in `hooks` Table

Ensure a hook is configured with the correct URL in the `hooks` table. This hook will be used for notifications.

---

## 6. Specify `notificationTopic` in `taskRoute`

Ensure the `notificationTopic` is correctly specified in the `taskRoute`. Example:
```json
"notificationTopic": "after_extract"
```

---

## Summary of Steps

1. **Create Key Vault Key**:
   - Use Azure CLI to create the signing key.
2. **Download Certificate**:
   - Use the orchestrator API to download the certificate and send it to the client.
3. **Add `NOTIFICATION_AUTH` Setting**:
   - Populate the JSON structure with client-provided details.
4. **Add `NOTIFICATION_SIGNER_KEYNAME` Setting**:
   - Specify the name of the Key Vault key.
5. **Configure Hook**:
   - Ensure the hook URL is correctly set in the `hooks` table.
6. **Set `notificationTopic`**:
   - Specify the topic in the `taskRoute`.
