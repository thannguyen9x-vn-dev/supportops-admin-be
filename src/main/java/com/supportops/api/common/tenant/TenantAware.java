package com.supportops.api.common.tenant;

import java.util.UUID;

public interface TenantAware {

    UUID getTenantId();

    void setTenantId(UUID tenantId);
}
