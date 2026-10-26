package com.papererp.common.domain;

public enum RollStatus {
    AVAILABLE,
    IN_PRODUCTION,
    FULLY_SLIT,
    CONSUMED,
    RESERVED,
    DISPATCHED,
    SCRAPPED;

    public boolean isEligibleForSlitting() {
        return this == AVAILABLE;
    }

    public boolean isEligibleForDispatch() {
        return this == AVAILABLE || this == RESERVED;
    }
}
