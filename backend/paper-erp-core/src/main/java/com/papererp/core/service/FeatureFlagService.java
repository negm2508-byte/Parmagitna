package com.papererp.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FeatureFlagService {

    @Value("${papererp.features.maker-checker-enforced:false}")
    private boolean makerCheckerEnforced;

    @Value("${papererp.features.outbox-worker-enabled:true}")
    private boolean outboxWorkerEnabled;

    @Value("${papererp.features.strict-scale-tolerance:true}")
    private boolean strictScaleTolerance;

    public boolean isMakerCheckerEnforced() {
        return makerCheckerEnforced;
    }

    public boolean isOutboxWorkerEnabled() {
        return outboxWorkerEnabled;
    }

    public boolean isStrictScaleTolerance() {
        return strictScaleTolerance;
    }
}
