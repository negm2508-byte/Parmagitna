package com.papererp.common.exception;

public class IneligibleRollException extends DomainException {

    public IneligibleRollException(String message) {
        super("INELIGIBLE_ROLL_STATE", message, 422);
    }
}
