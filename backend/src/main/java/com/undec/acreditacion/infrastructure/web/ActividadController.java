package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.CompletarActividadCommand;
import com.undec.acreditacion.application.input.CompletarActividadInformarProcesoUseCase;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.TipoDocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.DocumentoAcreditacionRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.ActividadDetalleResponse;
import com.undec.acreditacion.infrastructure.web.dto.DocumentoAcreditacionResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/actividades")
public class ActividadController {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    private static final List<DateTimeFormatter> FORMATOS_FECHA_HORA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MM/yyyy h:mm a").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd/MM/yyyy hh:mm a").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/dd/yyyy h:mm a").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("MM/dd/yyyy hh:mm a").toFormatter(Locale.ENGLISH)
    );

    private static final List<DateTimeFormatter> FORMATOS_SOLO_FECHA = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private final CompletarActividadInformarProcesoUseCase completarActividadUseCase;
    private final DocumentoAcreditacionRepositoryPort documentoRepository;
    private final UserRepository userRepository;

    public ActividadController(
            CompletarActividadInformarProcesoUseCase completarActividadUseCase,
            DocumentoAcreditacionRepositoryPort documentoRepository,
            UserRepository userRepository) {
        this.completarActividadUseCase = completarActividadUseCase;
        this.documentoRepository = documentoRepository;
        this.userRepository = userRepository;
    }

    @PostMapping(value = "/{id}/completar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('ACTIVIDAD_MANAGE')")
    public ResponseEntity<ActividadDetalleResponse> completarActividad(
            @PathVariable UUID id,
            @RequestParam("fechaReunion") String fechaReunionStr,
            @RequestParam("sintesisAcuerdos") String sintesisAcuerdos,
            @RequestParam("tipoDocumento") String tipoDocumentoStr,
            @RequestParam("archivo") MultipartFile archivo
    ) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainValidationException("El archivo probatorio es obligatorio");
        }

        String mimeType = archivo.getContentType();
        if (mimeType == null || !DocumentoAcreditacion.MIME_PDF.equalsIgnoreCase(mimeType.trim())) {
            // También validar por extensión si el navegador no setea el mime exacto
            if (archivo.getOriginalFilename() == null || !archivo.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new DomainValidationException("Solo se admiten documentos en formato PDF");
            }
            mimeType = DocumentoAcreditacion.MIME_PDF;
        }

        if (archivo.getSize() > DocumentoAcreditacion.MAX_TAMANIO_BYTES) {
            throw new DomainValidationException("El archivo debe pesar entre 1 byte y 20 MB");
        }

        Instant fechaReunion = parsearFechaReunion(fechaReunionStr);

        TipoDocumentoAcreditacion tipoDocumento;
        try {
            tipoDocumento = TipoDocumentoAcreditacion.valueOf(tipoDocumentoStr.trim().toUpperCase());
        } catch (Exception ex) {
            throw new DomainValidationException("Tipo de documento inválido: " + tipoDocumentoStr);
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();

        CompletarActividadCommand command = new CompletarActividadCommand(
                id,
                fechaReunion,
                sintesisAcuerdos,
                tipoDocumento,
                archivo.getBytes(),
                archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento.pdf",
                DocumentoAcreditacion.MIME_PDF,
                usuarioId
        );

        ActividadPlanTrabajo actividad = completarActividadUseCase.completar(command);

        List<DocumentoAcreditacion> docs = documentoRepository.findByActividadId(actividad.getId());
        List<DocumentoAcreditacionResponse> docsDto = docs.stream().map(d ->
                new DocumentoAcreditacionResponse(
                        d.getId(),
                        d.getNombreOriginal(),
                        d.getTipoDocumento().name(),
                        d.getMimeType(),
                        d.getTamanioBytes(),
                        "/api/documentos/" + d.getId() + "/descargar",
                        d.getCreadoEn()
                )
        ).collect(Collectors.toList());

        String nombreUsuario = userRepository.findById(usuarioId)
                .map(User::getUsername)
                .orElse("Usuario AEA");

        ActividadDetalleResponse response = new ActividadDetalleResponse(
                actividad.getId(),
                actividad.getPlanTrabajoId(),
                actividad.getNumeroOrden(),
                actividad.getCodigoActividad(),
                actividad.getNombre(),
                actividad.getEstado().name(),
                actividad.getFechaReunionCoordinacion(),
                actividad.getSintesisAcuerdos(),
                actividad.getCompletadaEn(),
                actividad.getCompletadaPor(),
                nombreUsuario,
                actividad.getHoja() != null ? actividad.getHoja().name() : "INFO",
                actividad.getFechaInicioEstimada(),
                actividad.getFechaFinEstimada(),
                actividad.getResponsable(),
                actividad.getPrioridad() != null ? actividad.getPrioridad().name() : "MEDIA",
                actividad.getCreadoEn(),
                actividad.getActualizadoEn(),
                docsDto
        );

        return ResponseEntity.ok(response);
    }

    private Instant parsearFechaReunion(String fechaStr) {
        if (fechaStr == null || fechaStr.trim().isEmpty()) {
            throw new DomainValidationException("La fecha de reunión es obligatoria");
        }
        String valor = fechaStr.trim();

        // a) Formatos ISO completos con offset o UTC
        try {
            return Instant.parse(valor);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(valor).toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // b), c) y d) Formatos con fecha y hora (HTML5 datetime-local, habituales argentinos y 12h)
        for (DateTimeFormatter formatter : FORMATOS_FECHA_HORA) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(valor, formatter);
                return ldt.atZone(ZONA_ARGENTINA).toInstant();
            } catch (DateTimeParseException ignored) {
            }
        }

        // c) y e) Formatos de solo fecha (dd/MM/yyyy, dd-MM-yyyy, yyyy-MM-dd)
        for (DateTimeFormatter formatter : FORMATOS_SOLO_FECHA) {
            try {
                LocalDate ld = LocalDate.parse(valor, formatter);
                return ld.atStartOfDay(ZONA_ARGENTINA).toInstant();
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new DomainValidationException(
                "Formato inválido de fecha de reunión: '" + valor + "'. " +
                "Formatos aceptados: ISO-8601 UTC u Offset (ej. 2026-03-10T10:00:00Z), " +
                "HTML5 datetime-local (yyyy-MM-ddTHH:mm), DD/MM/AAAA HH:mm, DD-MM-AAAA HH:mm, " +
                "DD/MM/AAAA, DD-MM-AAAA, MM/DD/AAAA h:mm a, o YYYY-MM-DD."
        );
    }

    private UUID resolverUsuarioAutenticadoId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
