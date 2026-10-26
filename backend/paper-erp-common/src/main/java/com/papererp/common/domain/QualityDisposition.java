package com.papererp.common.domain;

public enum QualityDisposition {
    PENDING,
    RELEASED,
    QUARANTINE,
    REJECTED,
    DOWNGRADED;

    public boolean isEligibleForDispatch() {
        return this == RELEASED || this == DOWNGRADED;
    }
}
