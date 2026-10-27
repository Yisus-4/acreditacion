package com.undec.acreditacion.infrastructure.web;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undec.acreditacion.application.input.*;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.infrastructure.web.dto.GuardarRegistroCaBorradorRequest;
import com.undec.acreditacion.infrastructure.web.dto.ObservarRegistroCaRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/convocatorias/{procesoId}/comision-autoevaluacion")
public class ComisionAutoevaluacionController {

    private final ConsultarRegistroCaUseCase consultarRegistroCaUseCase;
    private final SolicitarConformacionCaUseCase solicitarConformacionCaUseCase;
    private final GuardarRegistroCaBorradorUseCase guardarRegistroCaBorradorUseCase;
    private final RemitirRegistroCaUseCase remitirRegistroCaUseCase;
    private final AprobarRegistroCaUseCase aprobarRegistroCaUseCase;
    private final ObservarRegistroCaUseCase observarRegistroCaUseCase;
    private final DocumentStoragePort storagePort;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ComisionAutoevaluacionController(
            ConsultarRegistroCaUseCase consultarRegistroCaUseCase,
            SolicitarConformacionCaUseCase solicitarConformacionCaUseCase,
            GuardarRegistroCaBorradorUseCase guardarRegistroCaBorradorUseCase,
            RemitirRegistroCaUseCase remitirRegistroCaUseCase,
            AprobarRegistroCaUseCase aprobarRegistroCaUseCase,
            ObservarRegistroCaUseCase observarRegistroCaUseCase,
            DocumentStoragePort storagePort,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.consultarRegistroCaUseCase = consultarRegistroCaUseCase;
        this.solicitarConformacionCaUseCase = solicitarConformacionCaUseCase;
        this.guardarRegistroCaBorradorUseCase = guardarRegistroCaBorradorUseCase;
        this.remitirRegistroCaUseCase = remitirRegistroCaUseCase;
        this.aprobarRegistroCaUseCase = aprobarRegistroCaUseCase;
        this.observarRegistroCaUseCase = observarRegistroCaUseCase;
        this.storagePort = storagePort;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<RegistroCaDto> consultar(@PathVariable UUID procesoId) {
        RegistroCaDto res = consultarRegistroCaUseCase.consultarPorProcesoId(procesoId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/solicitar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroCaDto> solicitar(@PathVariable UUID procesoId) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        RegistroCaDto res = solicitarConformacionCaUseCase.solicitar(procesoId, usuarioId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/instructivo-it001")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargarInstructivoIt001() throws IOException {
        Resource resource = new ClassPathResource("instructivos/IT-001_Instructivo_Cuentas_CVar_CONEAU_Global.pdf");
        if (!resource.exists()) {
            throw new ResourceNotFoundException("No se encontró el instructivo oficial IT-001");
        }
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"IT-001_Instructivo_Cuentas_CVar_CONEAU_Global.pdf\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                    .body(bytes);
        }
    }

    @PutMapping("/borrador")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroCaDto> guardarBorrador(
            @PathVariable UUID procesoId,
            @RequestBody GuardarRegistroCaBorradorRequest req
    ) {
        RegistroCaDto res = guardarRegistroCaBorradorUseCase.guardarBorrador(
                procesoId, req.coincideCurricular(), req.miembros()
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/remitir", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroCaDto> remitir(
            @PathVariable UUID procesoId,
            @RequestParam(value = "coincideCurricular", defaultValue = "false") boolean coincideCurricular,
            @RequestParam(value = "miembros", required = false) String miembrosJson,
            @RequestParam(value = "resolucion", required = false) MultipartFile resolucion
    ) throws IOException {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        List<MiembroCaInputDto> miembros = List.of();
        if (miembrosJson != null && !miembrosJson.isBlank()) {
            miembros = objectMapper.readValue(miembrosJson, new TypeReference<List<MiembroCaInputDto>>() {});
        }

        byte[] resolucionBytes = (resolucion != null && !resolucion.isEmpty()) ? resolucion.getBytes() : null;
        String resolucionNombre = (resolucion != null && !resolucion.isEmpty()) ? resolucion.getOriginalFilename() : null;

        RegistroCaDto res = remitirRegistroCaUseCase.remitir(
                procesoId, usuarioId, coincideCurricular, miembros, resolucionBytes, resolucionNombre
        );
        return ResponseEntity.ok(res);
    }

    @PostMapping("/aprobar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroCaDto> aprobar(@PathVariable UUID procesoId) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        RegistroCaDto res = aprobarRegistroCaUseCase.aprobar(procesoId, usuarioId);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/observar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA') or hasAuthority('CONVOCATORIA_WRITE')")
    public ResponseEntity<RegistroCaDto> observar(
            @PathVariable UUID procesoId,
            @RequestBody ObservarRegistroCaRequest req
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        RegistroCaDto res = observarRegistroCaUseCase.observar(procesoId, usuarioId, req.observaciones());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/resolucion/descargar")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'CAREER_DIRECTOR', 'SCHOOL_DIRECTOR') or hasAuthority('CONVOCATORIA_READ')")
    public ResponseEntity<byte[]> descargarResolucion(@PathVariable UUID procesoId) {
        RegistroCaDto ca = consultarRegistroCaUseCase.consultarPorProcesoId(procesoId);
        if (ca.resolucionStoragePath() == null || ca.resolucionStoragePath().isBlank()) {
            throw new ResourceNotFoundException("No se ha adjuntado ninguna resolución de designación para este proceso");
        }
        byte[] bytes = storagePort.downloadFile(ca.resolucionStoragePath());
        String filename = ca.resolucionNombreOriginal() != null ? ca.resolucionNombreOriginal() : "Resolucion_Designacion_CA.pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(bytes.length))
                .body(bytes);
    }

    private Optional<User> resolverUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt;
            }
        }
        return userRepository.findByEmail("admin@undec.edu.ar");
    }

    private UUID resolverUsuarioAutenticadoId() {
        return resolverUsuarioAutenticado()
                .map(User::getId)
                .orElseGet(() -> userRepository.findAll().stream().findFirst().map(User::getId)
                        .orElse(UUID.fromString("00000000-0000-0000-0000-000000000001")));
    }
}
