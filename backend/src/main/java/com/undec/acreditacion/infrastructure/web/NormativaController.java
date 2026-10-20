package com.undec.acreditacion.infrastructure.web;

import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.*;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.DocumentoNormativaRepositoryPort;
import com.undec.acreditacion.infrastructure.web.dto.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping
public class NormativaController {

    private final SolicitarNormativaUseCase solicitarNormativaUseCase;
    private final ObtenerNormativaDetalleUseCase obtenerNormativaDetalleUseCase;
    private final GuardarDocumentoBorradorUseCase guardarDocumentoBorradorUseCase;
    private final EliminarDocumentoBorradorUseCase eliminarDocumentoBorradorUseCase;
    private final EnviarNormativaDefinitivaUseCase enviarNormativaDefinitivaUseCase;
    private final RevisarNormativaUseCase revisarNormativaUseCase;
    private final ExportarHojaInfoExcelUseCase exportarHojaInfoExcelUseCase;
    private final DocumentoNormativaRepositoryPort documentoRepository;
    private final DocumentStoragePort storagePort;
    private final UserRepository userRepository;

    public NormativaController(
            SolicitarNormativaUseCase solicitarNormativaUseCase,
            ObtenerNormativaDetalleUseCase obtenerNormativaDetalleUseCase,
            GuardarDocumentoBorradorUseCase guardarDocumentoBorradorUseCase,
            EliminarDocumentoBorradorUseCase eliminarDocumentoBorradorUseCase,
            EnviarNormativaDefinitivaUseCase enviarNormativaDefinitivaUseCase,
            RevisarNormativaUseCase revisarNormativaUseCase,
            ExportarHojaInfoExcelUseCase exportarHojaInfoExcelUseCase,
            DocumentoNormativaRepositoryPort documentoRepository,
            DocumentStoragePort storagePort,
            UserRepository userRepository
    ) {
        this.solicitarNormativaUseCase = solicitarNormativaUseCase;
        this.obtenerNormativaDetalleUseCase = obtenerNormativaDetalleUseCase;
        this.guardarDocumentoBorradorUseCase = guardarDocumentoBorradorUseCase;
        this.eliminarDocumentoBorradorUseCase = eliminarDocumentoBorradorUseCase;
        this.enviarNormativaDefinitivaUseCase = enviarNormativaDefinitivaUseCase;
        this.revisarNormativaUseCase = revisarNormativaUseCase;
        this.exportarHojaInfoExcelUseCase = exportarHojaInfoExcelUseCase;
        this.documentoRepository = documentoRepository;
        this.storagePort = storagePort;
        this.userRepository = userRepository;
    }

    @PostMapping({"/api/v1/procesos/{procesoId}/normativa", "/api/convocatorias/{procesoId}/normativa"})
    @PreAuthorize("@normativaSecurity.canAccess(authentication, #procesoId)")
    public ResponseEntity<NormativaDetalleResponse> solicitarNormativa(
            @PathVariable UUID procesoId,
            @RequestBody(required = false) SolicitarNormativaRequest request
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        String obs = request != null ? request.observaciones() : null;

        NormativaDetalleDto detalle = solicitarNormativaUseCase.ejecutar(
                new SolicitarNormativaCommand(procesoId, usuarioId, obs)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearDetalleResponse(detalle));
    }

    @GetMapping({"/api/v1/procesos/{procesoId}/normativa", "/api/convocatorias/{procesoId}/normativa"})
    @PreAuthorize("@normativaSecurity.canAccess(authentication, #procesoId)")
    public ResponseEntity<NormativaDetalleResponse> obtenerDetalle(@PathVariable UUID procesoId) {
        NormativaDetalleDto detalle = obtenerNormativaDetalleUseCase.ejecutar(procesoId);
        return ResponseEntity.ok(mapearDetalleResponse(detalle));
    }

    @PostMapping(value = {"/api/v1/procesos/{procesoId}/normativa/documentos", "/api/convocatorias/{procesoId}/normativa/documentos"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@normativaSecurity.canManage(authentication, #procesoId)")
    public ResponseEntity<DocumentoNormativaResponse> cargarBorrador(
            @PathVariable UUID procesoId,
            @RequestParam("tipoDocumento") String tipoDocumentoStr,
            @RequestParam("archivo") MultipartFile archivo
    ) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw new DomainValidationException("El archivo probatorio es obligatorio");
        }
        if (archivo.getSize() > 20L * 1024 * 1024) {
            throw new DomainValidationException("El archivo excede el tamaño máximo permitido de 20 MB");
        }

        TipoDocumentoNormativa tipoDocumento;
        try {
            tipoDocumento = TipoDocumentoNormativa.valueOf(tipoDocumentoStr.trim().toUpperCase());
        } catch (Exception ex) {
            throw new DomainValidationException("Tipo de documento normativo inválido: " + tipoDocumentoStr);
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();
        GuardarDocumentoBorradorCommand command = new GuardarDocumentoBorradorCommand(
                procesoId,
                tipoDocumento,
                archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "documento.pdf",
                archivo.getBytes(),
                archivo.getContentType() != null ? archivo.getContentType() : "application/pdf",
                usuarioId
        );

        DocumentoNormativaDto dto = guardarDocumentoBorradorUseCase.ejecutar(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapearDocumentoResponse(dto, true));
    }

    @DeleteMapping({"/api/v1/procesos/{procesoId}/normativa/documentos/{documentoId}", "/api/convocatorias/{procesoId}/normativa/documentos/{documentoId}"})
    @PreAuthorize("@normativaSecurity.canManage(authentication, #procesoId)")
    public ResponseEntity<Void> eliminarBorrador(
            @PathVariable UUID procesoId,
            @PathVariable UUID documentoId
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        eliminarDocumentoBorradorUseCase.ejecutar(new EliminarDocumentoBorradorCommand(procesoId, documentoId, usuarioId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping({"/api/v1/procesos/{procesoId}/normativa/enviar", "/api/convocatorias/{procesoId}/normativa/enviar"})
    @PreAuthorize("@normativaSecurity.canManage(authentication, #procesoId)")
    public ResponseEntity<NormativaDetalleResponse> enviarDefinitiva(@PathVariable UUID procesoId) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        NormativaDetalleDto detalle = enviarNormativaDefinitivaUseCase.ejecutar(new EnviarNormativaDefinitivaCommand(procesoId, usuarioId));
        return ResponseEntity.ok(mapearDetalleResponse(detalle));
    }

    @PostMapping({"/api/v1/procesos/{procesoId}/normativa/revisar", "/api/convocatorias/{procesoId}/normativa/revisar"})
    @PreAuthorize("@normativaSecurity.canReview(authentication)")
    public ResponseEntity<NormativaDetalleResponse> revisarNormativa(
            @PathVariable UUID procesoId,
            @RequestBody RevisarNormativaRequest request
    ) {
        if (request == null || request.decision() == null) {
            throw new DomainValidationException("La decisión de revisión es obligatoria (APROBADO o REQUIERE_CORRECCIONES)");
        }
        DecisionRevisionNormativa decision;
        String rawDecision = request.decision().trim().toUpperCase();
        if ("APROBADO".equals(rawDecision) || "APROBAR".equals(rawDecision)) {
            decision = DecisionRevisionNormativa.APROBAR;
        } else if ("REQUIERE_CORRECCIONES".equals(rawDecision) || "OBSERVAR".equals(rawDecision) || "OBSERVADO".equals(rawDecision)) {
            decision = DecisionRevisionNormativa.OBSERVAR;
        } else {
            try {
                decision = DecisionRevisionNormativa.valueOf(rawDecision);
            } catch (Exception ex) {
                throw new DomainValidationException("Decisión inválida: " + request.decision());
            }
        }

        UUID usuarioId = resolverUsuarioAutenticadoId();
        NormativaDetalleDto detalle = revisarNormativaUseCase.ejecutar(new RevisarNormativaCommand(
                procesoId,
                usuarioId,
                decision,
                request.observaciones(),
                request.documentosObservados()
        ));

        return ResponseEntity.ok(mapearDetalleResponse(detalle));
    }

    @GetMapping("/api/v1/planes-trabajo/{planTrabajoId}/hoja-info")
    @PreAuthorize("@normativaSecurity.canExportHojaInfo(authentication)")
    public ResponseEntity<byte[]> exportarHojaInfo(@PathVariable UUID planTrabajoId) {
        byte[] contenidoExcel = exportarHojaInfoExcelUseCase.ejecutar(planTrabajoId);
        String filename = "Hoja_INFO_PlanTrabajo_" + planTrabajoId + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenidoExcel);
    }

    @GetMapping({"/api/v1/procesos/{procesoId}/normativa/documentos/{documentoId}/descargar", "/api/convocatorias/{procesoId}/normativa/documentos/{documentoId}/descargar"})
    @PreAuthorize("@normativaSecurity.canAccess(authentication, #procesoId)")
    public ResponseEntity<?> descargarDocumento(
            @PathVariable UUID procesoId,
            @PathVariable UUID documentoId
    ) {
        Optional<DocumentoNormativa> docOpt = documentoRepository.findById(documentoId);
        if (docOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        DocumentoNormativa doc = docOpt.get();

        String presignedUrl = storagePort.getPresignedDownloadUrl(doc.getStoragePath(), 3600);
        if (presignedUrl != null && !presignedUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presignedUrl))
                    .build();
        }

        byte[] contenido = storagePort.downloadFile(doc.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNombreOriginal() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(contenido);
    }

    private NormativaDetalleResponse mapearDetalleResponse(NormativaDetalleDto dto) {
        Set<TipoDocumentoNormativa> requeridos = MatrizNormativa.obtenerRequeridos(dto.carreraEsNueva(), dto.carreraTieneHistorial());
        List<DocumentoNormativaDto> listaDocs = dto.documentos() != null ? dto.documentos() : List.of();

        List<DocumentoNormativaResponse> docs = listaDocs.stream()
                .map(d -> mapearDocumentoResponse(d, requeridos.contains(d.tipoDocumento())))
                .toList();

        long cargadosRequeridos = listaDocs.stream()
                .map(DocumentoNormativaDto::tipoDocumento)
                .filter(requeridos::contains)
                .distinct()
                .count();

        double avance = requeridos.isEmpty() ? 100.0 : ((double) cargadosRequeridos / requeridos.size()) * 100.0;

        List<String> faltantes = MatrizNormativa.obtenerFaltantes(
                listaDocs.stream().map(DocumentoNormativaDto::tipoDocumento).toList(),
                dto.carreraEsNueva(),
                dto.carreraTieneHistorial()
        ).stream().map(Enum::name).toList();

        return new NormativaDetalleResponse(
                dto.id(),
                dto.procesoId(),
                dto.actividadId(),
                dto.estado().name(),
                dto.fechaSolicitud(),
                dto.fechaLimiteEntrega(),
                dto.diasHabilesPlazo(),
                dto.badgePlazo(),
                avance,
                (int) cargadosRequeridos,
                requeridos.size(),
                dto.observacionesSolicitud(),
                dto.observacionesRevision(),
                docs,
                faltantes,
                dto.carreraEsNueva(),
                dto.carreraTieneHistorial()
        );
    }

    private DocumentoNormativaResponse mapearDocumentoResponse(DocumentoNormativaDto d, boolean esObligatorio) {
        return new DocumentoNormativaResponse(
                d.id(),
                d.solicitudNormativaId(),
                d.procesoId(),
                d.tipoDocumento().name(),
                d.carpetaDestino().name(),
                d.nombreOriginal(),
                d.storagePath(),
                d.mimeType(),
                d.tamanioBytes(),
                d.esBorrador(),
                d.observado(),
                d.motivoObservacion(),
                esObligatorio,
                d.creadoEn(),
                d.actualizadoEn()
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
