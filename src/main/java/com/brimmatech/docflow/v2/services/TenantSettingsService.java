package com.brimmatech.docflow.v2.services;

import com.brimmatech.docflow.enums.SettingsCategory;
import com.brimmatech.docflow.exception.DocflowDataException;
import com.brimmatech.docflow.v2.models.TenantSettings;
import com.brimmatech.docflow.v2.repository.TenantSettingsRepository;
import com.brimmatech.general.types.ThrowingSupplier;
import com.brimmatech.general.utils.OptionUtils;
import com.brimmatech.saas.crytograph.EncryptDecryptService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.brimmatech.docflow.enums.SettingsCategory.Constants.SYS_PROMPTS;

@Service @RequiredArgsConstructor @Slf4j public class TenantSettingsService {


    public static final String GLOBAL_TENANT_NAME = "__vdx__";
    public static TaskProcessorConfig DEFAULT_TASK_PROCESSOR_CONFIG = TaskProcessorConfig.builder().pullSize(1).bufferSize(50).workerSize(15).build();

    private final TenantSettingsRepository tenantSettingsRepository;
    private final ObjectMapper objectMapper;
    private final EncryptDecryptService encryptDecryptService;
    private final Map<CachedSettingKey, TenantSettings> settingsCache = new ConcurrentHashMap<>();
    private boolean useCache = false;

    @EventListener(ApplicationStartedEvent.class)
    public void init() {
        useCache =
                getGlobalSetting(SettingsCategory.SYSTEM_SETTINGS_USE_CACHE).flatMap(v -> typedMeta(v, Boolean.class))
                        .orElse(Boolean.FALSE);
    }

    public <T> Optional<T> getSettingTyped(long tenantId, SettingsCategory settingName, TypeReference<T> clazz) {
        return getSetting(tenantId, settingName).or(() -> getGlobalSetting(settingName))
                .flatMap(v -> this.typedMetaFromType(v, clazz));
    }

    public <T> Optional<T> getSettingTyped(long tenantId, SettingsCategory settingName, Class<T> clazz) {
        return getSetting(tenantId, settingName).or(() -> getGlobalSetting(settingName))
                .flatMap(v -> this.typedMeta(v, clazz));
    }

    public <T> T getSettingTyped(long tenantId, SettingsCategory settingName, Class<T> clazz, T defaults) {
        return getSetting(tenantId, settingName).or(() -> getGlobalSetting(settingName))
                .flatMap(v -> this.typedMeta(v, clazz)).orElse(defaults);
    }

    public <T> Optional<T> getGlobalSettingTyped(SettingsCategory settingName, Class<T> clazz) {
        return getGlobalSetting(settingName).flatMap(v -> this.typedMeta(v, clazz));
    }

    public Optional<TenantSettings> getSetting(long tenantId, SettingsCategory settingName) {
        val cacheKey = CachedSettingKey.builder().tenantId(tenantId).settingsCategory(settingName).build();

        if (useCache && settingsCache.containsKey(cacheKey)) {
            return Optional.ofNullable(settingsCache.get(cacheKey));
        }
        val value = tenantSettingsRepository.findByTenantIdAndCategoryAndStrategy(tenantId,
                settingName.getName(),
                settingName.getStrategy());

        if (useCache) {
            value.ifPresent((v) -> {
                settingsCache.put(cacheKey, v);
            });
        }
        return value;
    }

    public <T> Optional<T> typedMetaFromType(TenantSettings v, TypeReference<T> clazz) {
        return ThrowingSupplier.getCapturingExceptions(() -> objectMapper.treeToValue(v.getMeta(), clazz));
    }

    private <T> Optional<T> typedMeta(TenantSettings v, Class<T> clazz) {
        return ThrowingSupplier.getCapturingExceptions(() -> objectMapper.treeToValue(v.getMeta(), clazz));
    }

    public Optional<String> getSystemPromptWithGlobalDefaults(long tenantId, SettingsCategory sysPromptSetting) {
        return validateSettingNamespacedOnPrompts(sysPromptSetting).flatMap((_x_) -> getSetting(tenantId,
                sysPromptSetting).or(() -> getGlobalSetting(sysPromptSetting))
                .flatMap(setting -> ThrowingSupplier.getCapturingExceptions(() -> setting.getMeta().asText()))
                .map(v -> new String(Base64.getDecoder().decode(v))));
    }

    private Optional<Boolean> validateSettingNamespacedOnPrompts(SettingsCategory sysPromptSetting) {
        return OptionUtils.fromBoolean(sysPromptSetting.getName().equals(SYS_PROMPTS));
    }

    public Optional<TenantSettings> getGlobalSetting(SettingsCategory settingName) {
        return tenantSettingsRepository.findGlobalSetting(GLOBAL_TENANT_NAME,
                settingName.getName(),
                settingName.getStrategy());
    }


    public Optional<MailConfigDto> getMailServerDetails(long tenantId) {
        Optional<TenantSettings> tenantSettingsOptional = tenantSettingsRepository
                .findByTenantIdAndCategoryAndStrategy(tenantId, "config", "notification");

        MailConfigDto mailConfigDto = null;
        if (tenantSettingsOptional.isPresent()) {
            Object decrypt = encryptDecryptService.decrypt(tenantSettingsOptional.get().getMeta().asText());
            mailConfigDto = objectMapper.convertValue(decrypt, MailConfigDto.class);
        }
        return Optional.ofNullable(mailConfigDto);
    }


    private void applyAdd(JsonNode parent, String key, JsonNode value) {
        if (parent.isObject() && key.matches("\\d+")) {
            JsonNode config = parent.at("/route/config");
            if (config.isArray()) {
                ((ArrayNode) config).insert(Integer.parseInt(key), value);
                return;
            }
        }
        ((ArrayNode) parent).insert(Integer.parseInt(key), value);
    }

    private void applyEdit(JsonNode root, String targetPath, JsonNode value) {

        String clean = targetPath.startsWith("/") ? targetPath.substring(1) : targetPath;
        int lastSlash = clean.lastIndexOf('/');
        String parentPtr = (lastSlash == -1) ? "" : clean.substring(0, lastSlash);
        String key = (lastSlash == -1) ? clean : clean.substring(lastSlash + 1);

        JsonNode parent = parentPtr.isEmpty() ? root : root.at("/" + parentPtr);
        if (parent.isMissingNode()) {
            throw new DocflowDataException("Invalid EDIT parent pointer: " + parentPtr);
        }

        if (parent instanceof ObjectNode obj) {

            JsonNode existing = obj.get(key);

            if (existing != null && existing.isObject() && value.isObject()) {
                ObjectNode existingObj = (ObjectNode) existing;
                value.fields().forEachRemaining(entry -> existingObj.set(entry.getKey(), entry.getValue()));
                return;
            }

            obj.set(key, value);
            return;
        }
        throw new DocflowDataException("EDIT failed at path: " + targetPath);
    }

    private void applyDelete(JsonNode parent, String key) {
        if (parent.isArray()) {
            ((ArrayNode) parent).remove(Integer.parseInt(key));
            return;
        }
        if (parent.isObject() && key.matches("\\d+")) {
            JsonNode config = parent.at("/route/config");
            if (config.isArray()) {
                ((ArrayNode) config).remove(Integer.parseInt(key));
                return;
            }
        }
        if (parent.isObject()) {
            ((ObjectNode) parent).remove(key);
            return;
        }
        throw new DocflowDataException("DELETE not supported for node type");
    }

    @Builder public record TaskProcessorConfig(int pullSize, int bufferSize, int workerSize) {
    }

    @Builder
    public record SuspensionStrategy(com.brimmatech.docflow.enums.SuspensionStrategy strategy) {
    }

    @Builder public record BlobConfigSetting(String connectionString, String containerName) {
    }

    @Builder public record MailConfigDto(String mailHost, String mailPort, String mailUsername,
                                         String mailPassword, String mailProvider, String mailSmtpAuthEnabled,
                                         String mailSmtpTlsEnabled) {
    }

    @Builder
    private record CachedSettingKey(long tenantId, SettingsCategory settingsCategory) {
    }

}
