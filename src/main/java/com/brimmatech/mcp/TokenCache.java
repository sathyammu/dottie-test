package com.brimmatech.mcp;

import com.brimmatech.encompass.tokengenerator.TokenResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class TokenCache {
    private TokenResponse tokenResponse;
    private Instant lastActivityTimestamp;

    public TokenCache(TokenResponse tokenResponse) {
        this.tokenResponse = tokenResponse;
        this.lastActivityTimestamp = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(lastActivityTimestamp.plusSeconds(30 * 60));
    }

    public boolean isInactive() {
        return Instant.now().isAfter(lastActivityTimestamp.plusSeconds(15 * 60));
    }

    public void updateToken(TokenResponse newToken) {
        this.tokenResponse = newToken;
        this.lastActivityTimestamp = Instant.now();
    }

    public TokenResponse getTokenResponse() {
        log.info("Updating last activity timestamp for token usage.");
        this.lastActivityTimestamp = Instant.now();
        return tokenResponse;
    }
}