package com.brimmatech.encompass.tokengenerator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    /**
     * This constructor allows parsing when the response is a plain string.
     */
    @JsonCreator
    public TokenResponse(String rawToken) {
        this.accessToken = rawToken;
        this.tokenType = "Bearer"; // default or null if not known
    }
}
