package com.undec.acreditacion.infrastructure.estandares;

import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(basePackages = {
        "com.undec.acreditacion.infrastructure.estandares",
        "com.undec.acreditacion.infrastructure.datoscentrales"
})
public class EstandarExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(EstandarExceptionHandler.class);

    @ExceptionHandler(BusinessRuleConflictException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleConflict(BusinessRuleConflictException ex) {
        log.warn("BusinessRuleConflict in estandares/datoscentrales: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "message", ex.getMessage(),
                        "status", HttpStatus.CONFLICT.value()
                ));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<Map<String, Object>> handleDomainValidation(DomainValidationException ex) {
        log.warn("DomainValidationException in estandares/datoscentrales: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "message", ex.getMessage(),
                        "status", HttpStatus.UNPROCESSABLE_ENTITY.value()
                ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("ResourceNotFoundException in estandares/datoscentrales: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "message", ex.getMessage(),
                        "status", HttpStatus.NOT_FOUND.value()
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(ResponseStatusException ex) {
        String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "error", msg,
                        "message", msg,
                        "status", ex.getStatusCode().value()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException in estandares/datoscentrales: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "message", ex.getMessage(),
                        "status", HttpStatus.BAD_REQUEST.value()
                ));
    }
}
