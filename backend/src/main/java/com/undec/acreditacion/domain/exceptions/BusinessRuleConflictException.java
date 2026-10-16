package com.undec.acreditacion.domain.exceptions;

public class BusinessRuleConflictException extends RuntimeException {

    public BusinessRuleConflictException(String message) {
        super(message);
    }
}
