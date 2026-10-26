package com.papererp.common.exception;

public class EntityNotFoundException extends DomainException {

    public EntityNotFoundException(String entityName, Object identifier) {
        super("ENTITY_NOT_FOUND", String.format("%s with identifier '%s' was not found", entityName, identifier), 404);
    }

    public EntityNotFoundException(String message) {
        super("ENTITY_NOT_FOUND", message, 404);
    }
}
