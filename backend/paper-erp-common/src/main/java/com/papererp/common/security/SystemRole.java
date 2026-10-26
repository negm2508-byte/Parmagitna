package com.papererp.common.security;

public enum SystemRole {
    ADMIN,
    PLANT_MANAGER,
    PRODUCTION_OPERATOR,
    QC_INSPECTOR,
    WAREHOUSE_KEEPER;

    public String asAuthority() {
        return "ROLE_" + this.name();
    }
}
