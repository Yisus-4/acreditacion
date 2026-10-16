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
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/actividades")
public class ActividadController {

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

        Instant fechaReunion;
        try {
            if (fechaReunionStr.contains("T")) {
                fechaReunion = Instant.parse(fechaReunionStr);
            } else {
                fechaReunion = LocalDate.parse(fechaReunionStr).atStartOfDay(ZoneOffset.UTC).toInstant();
            }
        } catch (Exception ex) {
            throw new DomainValidationException("Formato inválido de fecha de reunión (esperado ISO-8601 o YYYY-MM-DD)");
        }

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
                actividad.getCreadoEn(),
                actividad.getActualizadoEn(),
                docsDto
        );

        return ResponseEntity.ok(response);
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
