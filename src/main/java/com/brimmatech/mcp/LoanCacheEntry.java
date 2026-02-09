package com.brimmatech.mcp;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LoanCacheEntry {
    private final ConcurrentMap<CacheField, Object> fields = new ConcurrentHashMap<>();
    private Instant lastUpdated;
    private static final long CACHE_TTL_SECONDS = 30 * 60;

    public LoanCacheEntry() {
        this.lastUpdated = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(lastUpdated.plusSeconds(CACHE_TTL_SECONDS));
    }

    public void updateField(CacheField field, Object value) {
        fields.put(field, value);
        this.lastUpdated = Instant.now();
    }

    public Object getField(CacheField field) {
        return fields.get(field);
    }

    public enum CacheField {
        LOAN_ID,
        UNDERWRITING_CONDITIONS,
        PRELIMINARY_CONDITIONS,
        POST_CLOSING_CONDITIONS,
        ENHANCED_CONDITION,
        ILAD,
        ULADDU,
        ULADLPA
    }
    public enum MismoFormatTypes{
        ILAD,ULADDU,ULADLPA
    }
}
