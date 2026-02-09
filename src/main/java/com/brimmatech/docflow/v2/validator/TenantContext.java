package com.brimmatech.docflow.v2.validator;

import org.springframework.stereotype.Component;

@Component
public class TenantContext {

    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public  void setTenantName(String tenantName) {
        currentTenant.set(tenantName);
    }

    public  String getTenantName() {
        return currentTenant.get();
    }

    public  void clear() {
        currentTenant.remove();
    }
}
