package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.domain.exceptions.BusinessRuleConflictException;
import com.undec.acreditacion.domain.exceptions.DocumentosFaltantesException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.EstadoInvalidoException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice(basePackages = "com.undec.acreditacion.infrastructure.web")
public class ConvocatoriaExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ConvocatoriaExceptionHandler.class);

    @ExceptionHandler(BusinessRuleConflictException.class)
    public ResponseEntity<Map<String, String>> handleBusinessRuleConflict(BusinessRuleConflictException ex) {
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleEstadoInvalido(EstadoInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(DocumentosFaltantesException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentosFaltantes(DocumentosFaltantesException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "faltantes", ex.getDocumentosFaltantes().stream().map(Enum::name).toList()
                ));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "El archivo excede el tamaño máximo permitido de 20 MB"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        log.error("Error no controlado en el servidor: ", ex);
        String mensaje = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "Error interno del servidor";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", mensaje));
    }
}
