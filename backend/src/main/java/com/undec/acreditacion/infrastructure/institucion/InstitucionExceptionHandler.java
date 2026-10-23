package com.undec.acreditacion.infrastructure.institucion;

import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.InstitucionDuplicadaException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.undec.acreditacion.infrastructure.institucion")
public class InstitucionExceptionHandler {

    @ExceptionHandler(InstitucionDuplicadaException.class)
    public ResponseEntity<Map<String, String>> handleInstitucionDuplicada(InstitucionDuplicadaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Conflicto de integridad de datos: ya existe una institución con datos duplicados"));
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleArchivoInvalido(ArchivoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, String>> handleDomainValidation(DomainValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        HttpStatus status = ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(Map.of("error", ex.getMessage()));
    }
}
