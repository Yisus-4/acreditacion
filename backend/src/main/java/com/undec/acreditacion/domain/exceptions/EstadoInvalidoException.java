package com.undec.acreditacion.domain.exceptions;

public class EstadoInvalidoException extends DomainValidationException {

    public EstadoInvalidoException(String message) {
        super(message);
    }
}
