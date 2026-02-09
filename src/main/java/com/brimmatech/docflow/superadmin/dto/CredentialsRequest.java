package com.brimmatech.docflow.superadmin.dto;


import lombok.Data;

@Data
public class CredentialsRequest {
    private String client_id;
    private String client_secret;
    private String user_name;
    private String password;
    private String instance_id;
}
