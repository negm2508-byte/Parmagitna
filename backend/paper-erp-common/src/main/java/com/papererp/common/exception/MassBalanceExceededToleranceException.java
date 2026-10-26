package com.papererp.common.exception;

import java.math.BigDecimal;

public class MassBalanceExceededToleranceException extends DomainException {

    private final BigDecimal inputMassKg;
    private final BigDecimal outputMassKg;
    private final BigDecimal varianceKg;
    private final BigDecimal maxAllowedVarianceKg;

    public MassBalanceExceededToleranceException(
            BigDecimal inputMassKg,
            BigDecimal outputMassKg,
            BigDecimal varianceKg,
            BigDecimal maxAllowedVarianceKg,
            String message) {
        super("MASS_BALANCE_TOLERANCE_EXCEEDED", message, 422);
        this.inputMassKg = inputMassKg;
        this.outputMassKg = outputMassKg;
        this.varianceKg = varianceKg;
        this.maxAllowedVarianceKg = maxAllowedVarianceKg;
    }

    public BigDecimal getInputMassKg() {
        return inputMassKg;
    }

    public BigDecimal getOutputMassKg() {
        return outputMassKg;
    }

    public BigDecimal getVarianceKg() {
        return varianceKg;
    }

    public BigDecimal getMaxAllowedVarianceKg() {
        return maxAllowedVarianceKg;
    }
}
