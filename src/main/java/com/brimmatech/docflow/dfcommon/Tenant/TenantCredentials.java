package com.brimmatech.docflow.dfcommon.Tenant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TenantCredentials {

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("user_name")
    private String userName;

    private String password;

    @JsonProperty("instance_id")
    private String instanceId;

    @JsonProperty("graph_client_id")
    private String graphClientId;

    @JsonProperty("graph_client_secret")
    private String graphClientSecret;

    @JsonProperty("graph_tenant_id")
    private String graphTenantId;

    @JsonProperty("sharepoint_url")
    private String sharepointUrl;

    @JsonProperty("sharepoint_folder_name")
    private String sharepointFolderName;

    @JsonProperty("site_name")
    private String siteName;
}
