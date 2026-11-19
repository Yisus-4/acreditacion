package com.undec.acreditacion.infrastructure.autoevaluacion;

import com.undec.acreditacion.application.output.UserRepository;
import com.undec.acreditacion.domain.entities.ActividadPlanTrabajo;
import com.undec.acreditacion.domain.entities.PlanTrabajo;
import com.undec.acreditacion.domain.entities.User;
import com.undec.acreditacion.domain.ports.PlanTrabajoRepositoryPort;
import com.undec.acreditacion.infrastructure.autoevaluacion.dto.*;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.*;
import com.undec.acreditacion.infrastructure.persistence.postgres.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/autoevaluacion")
public class AutoevaluacionInfraestructuraController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AutoevaluacionInfraestructuraController.class);

    private final SpringDataInmuebleRepository inmuebleRepository;
    private final SpringDataTipoEspacioFisicoRepository tipoEspacioRepository;
    private final SpringDataEspacioFisicoRepository espacioFisicoRepository;
    private final SpringDataInmuebleDocumentacionRespaldoRepository documentacionRepository;
    private final SpringDataInstitucionRepository institucionRepository;
    private final SpringDataProvinciaRepository provinciaRepository;
    private final SpringDataDepartamentoRepository departamentoRepository;
    private final SpringDataLocalidadRepository localidadRepository;
    private final SpringDataCarreraRepository carreraRepository;
    private final SpringDataSedeLocalizacionRepository sedeRepository;
    private final UserRepository userRepository;
    private final PlanTrabajoRepositoryPort planTrabajoRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.undec.acreditacion.domain.ports.DocumentStoragePort documentStoragePort;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.undec.acreditacion.infrastructure.security.AuthenticatedUserProvider authenticatedUserProvider;

    public AutoevaluacionInfraestructuraController(
            SpringDataInmuebleRepository inmuebleRepository,
            SpringDataTipoEspacioFisicoRepository tipoEspacioRepository,
            SpringDataEspacioFisicoRepository espacioFisicoRepository,
            SpringDataInmuebleDocumentacionRespaldoRepository documentacionRepository,
            SpringDataInstitucionRepository institucionRepository,
            SpringDataProvinciaRepository provinciaRepository,
            SpringDataDepartamentoRepository departamentoRepository,
            SpringDataLocalidadRepository localidadRepository,
            SpringDataCarreraRepository carreraRepository,
            SpringDataSedeLocalizacionRepository sedeRepository,
            UserRepository userRepository,
            PlanTrabajoRepositoryPort planTrabajoRepository
    ) {
        this.inmuebleRepository = inmuebleRepository;
        this.tipoEspacioRepository = tipoEspacioRepository;
        this.espacioFisicoRepository = espacioFisicoRepository;
        this.documentacionRepository = documentacionRepository;
        this.institucionRepository = institucionRepository;
        this.provinciaRepository = provinciaRepository;
        this.departamentoRepository = departamentoRepository;
        this.localidadRepository = localidadRepository;
        this.carreraRepository = carreraRepository;
        this.sedeRepository = sedeRepository;
        this.userRepository = userRepository;
        this.planTrabajoRepository = planTrabajoRepository;
    }

    // -------------------------------------------------------------------------
    // Inmuebles - Padrón General y Maestro
    // -------------------------------------------------------------------------

    @GetMapping("/inmuebles")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmuebles(
            @RequestParam(name = "esMaestro", required = false) Boolean esMaestro
    ) {
        List<InmuebleJpaEntity> entidades = inmuebleRepository.findMaestrosActivos();
        if (entidades.isEmpty() && !Boolean.TRUE.equals(esMaestro)) {
            entidades = inmuebleRepository.findAllActivosConRelaciones();
        }
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/maestro")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmueblesMaestros() {
        List<InmuebleJpaEntity> entidades = inmuebleRepository.findMaestrosActivos();
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<InmuebleResponse> obtenerInmueble(@PathVariable UUID id) {
        return inmuebleRepository.findByIdCompleto(id)
                .map(this::mapToInmuebleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/inmuebles")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<InmuebleResponse> registrarInmueble(@RequestBody RegistrarInmuebleRequest request) {
        InstitucionJpaEntity institucion = institucionRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe institución registrada en el sistema"));

        InmuebleJpaEntity entity = new InmuebleJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setInstitucion(institucion);
        entity.setNumeroOrden(request.numeroOrden());
        entity.setNombre(request.nombre());
        entity.setDomicilio(request.domicilio() != null ? request.domicilio() : (request.calle() + " " + request.numero()));
        entity.setCalle(request.calle());
        entity.setNumero(request.numero());
        entity.setAnioConstruccion(request.anioConstruccion());
        entity.setAnioInicioInstitucion(request.anioInicioInstitucion());
        entity.setPropiedadInstitucion(request.propiedadInstitucion() != null ? request.propiedadInstitucion() : true);
        entity.setCondicionTenencia(request.condicionTenencia() != null ? request.condicionTenencia().toUpperCase() : "PROPIO");
        entity.setSituacion(request.situacion() != null ? request.situacion() : "Propiedad");
        entity.setVinculaTodasCarreras(request.vinculaTodasCarreras() != null ? request.vinculaTodasCarreras() : false);
        boolean esCopiaProceso = Boolean.FALSE.equals(request.esMaestro()) && request.procesoId() != null;
        entity.setEsMaestro(!esCopiaProceso);
        entity.setProcesoId(esCopiaProceso ? request.procesoId() : null);
        entity.setSuperficieCubiertaM2(request.superficieCubiertaM2());
        entity.setSuperficieTotalM2(request.superficieTotalM2());
        entity.setObservaciones(request.observaciones());
        entity.setEstadoConeau(request.estadoConeau() != null ? request.estadoConeau() : "CARGADO");
        entity.setVerificado(false);
        entity.setActivo(true);
        entity.setCreadoEn(Instant.now());
        entity.setActualizadoEn(Instant.now());

        if (request.sedeId() != null) {
            sedeRepository.findById(request.sedeId()).ifPresent(entity::setSede);
        } else {
            sedeRepository.findAll().stream().findFirst().ifPresent(entity::setSede);
        }

        if (request.provinciaId() != null) {
            provinciaRepository.findById(request.provinciaId()).ifPresent(entity::setProvincia);
        }
        if (entity.getProvincia() == null) {
            provinciaRepository.findAll().stream().findFirst().ifPresent(entity::setProvincia);
        }
        if (request.departamentoId() != null) {
            departamentoRepository.findById(request.departamentoId()).ifPresent(entity::setDepartamentoGeografico);
        }
        if (entity.getDepartamentoGeografico() == null) {
            departamentoRepository.findAll().stream().findFirst().ifPresent(entity::setDepartamentoGeografico);
        }
        if (request.localidadId() != null) {
            localidadRepository.findById(request.localidadId()).ifPresent(entity::setLocalidad);
        }
        if (entity.getLocalidad() == null) {
            localidadRepository.findAll().stream().findFirst().ifPresent(entity::setLocalidad);
        }

        if (request.carrerasIds() != null && !request.carrerasIds().isEmpty()) {
            Set<CarreraJpaEntity> carreras = new HashSet<>(carreraRepository.findAllById(request.carrerasIds()));
            entity.setCarreras(carreras);
        }

        InmuebleJpaEntity guardado = inmuebleRepository.save(entity);

        // Espacios físicos y subespacios (1 nivel)
        if (request.espacios() != null && !request.espacios().isEmpty()) {
            for (EspacioFisicoRequest espReq : request.espacios()) {
                EspacioFisicoJpaEntity esp = new EspacioFisicoJpaEntity();
                esp.setId(UUID.randomUUID());
                esp.setInmueble(guardado);
                String espDenom = (espReq.denominacion() != null && !espReq.denominacion().isBlank()) ? espReq.denominacion() :
                        (espReq.descripcionInfraestructura() != null && !espReq.descripcionInfraestructura().isBlank() ? espReq.descripcionInfraestructura() : "Espacio Físico");
                esp.setDenominacion(espDenom);
                esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura() != null ? espReq.descripcionInfraestructura() : espDenom);
                esp.setCantidad(espReq.cantidad() != null ? espReq.cantidad() : 1);
                int cap = espReq.capacidadPersonasSentadas() != null ? espReq.capacidadPersonasSentadas() :
                        (espReq.capacidadEstudiantes() != null ? espReq.capacidadEstudiantes() : 0);
                esp.setCapacidadPersonasSentadas(cap);
                esp.setCapacidadEstudiantes(cap);
                esp.setSuperficieM2(espReq.superficieM2());
                esp.setEquipamientoDisponible(espReq.equipamientoDisponible());
                esp.setObservacionHigieneSeguridad(espReq.observacionHigieneSeguridad());
                boolean tieneSubs = espReq.tieneSubespacios() != null && espReq.tieneSubespacios();
                esp.setTieneSubespacios(tieneSubs);
                esp.setEstadoConeau(espReq.estadoConeau() != null ? espReq.estadoConeau() : "CARGADO");
                esp.setEsMaestro(entity.getEsMaestro());
                esp.setProcesoId(entity.getProcesoId());
                esp.setActivo(true);
                esp.setCreadoEn(Instant.now());
                if (espReq.tipoEspacioId() != null) {
                    tipoEspacioRepository.findById(espReq.tipoEspacioId()).ifPresent(esp::setTipoEspacio);
                }
                if (esp.getTipoEspacio() == null) {
                    tipoEspacioRepository.findAll().stream().findFirst().ifPresent(esp::setTipoEspacio);
                }
                espacioFisicoRepository.save(esp);
                guardado.getEspacios().add(esp);

                // Subespacios de 1 nivel
                if (tieneSubs && espReq.subespacios() != null && !espReq.subespacios().isEmpty()) {
                    for (EspacioFisicoRequest subReq : espReq.subespacios()) {
                        EspacioFisicoJpaEntity sub = new EspacioFisicoJpaEntity();
                        sub.setId(UUID.randomUUID());
                        sub.setInmueble(guardado);
                        sub.setEspacioPadre(esp);
                        sub.setTieneSubespacios(false); // Regla: subespacio no puede tener subespacios
                        String subDenom = (subReq.denominacion() != null && !subReq.denominacion().isBlank()) ? subReq.denominacion() :
                                (subReq.descripcionInfraestructura() != null && !subReq.descripcionInfraestructura().isBlank() ? subReq.descripcionInfraestructura() : "Subespacio");
                        sub.setDenominacion(subDenom);
                        sub.setDescripcionInfraestructura(subReq.descripcionInfraestructura() != null ? subReq.descripcionInfraestructura() : subDenom);
                        sub.setCantidad(subReq.cantidad() != null ? subReq.cantidad() : 1);
                        int subCap = subReq.capacidadPersonasSentadas() != null ? subReq.capacidadPersonasSentadas() :
                                (subReq.capacidadEstudiantes() != null ? subReq.capacidadEstudiantes() : 0);
                        sub.setCapacidadPersonasSentadas(subCap);
                        sub.setCapacidadEstudiantes(subCap);
                        sub.setSuperficieM2(subReq.superficieM2());
                        sub.setEquipamientoDisponible(subReq.equipamientoDisponible());
                        sub.setObservacionHigieneSeguridad(subReq.observacionHigieneSeguridad());
                        sub.setEstadoConeau(subReq.estadoConeau() != null ? subReq.estadoConeau() : "CARGADO");
                        sub.setEsMaestro(entity.getEsMaestro());
                        sub.setProcesoId(entity.getProcesoId());
                        sub.setActivo(true);
                        sub.setCreadoEn(Instant.now());
                        if (subReq.tipoEspacioId() != null) {
                            tipoEspacioRepository.findById(subReq.tipoEspacioId()).ifPresent(sub::setTipoEspacio);
                        }
                        if (sub.getTipoEspacio() == null) {
                            sub.setTipoEspacio(esp.getTipoEspacio());
                        }
                        if (sub.getTipoEspacio() == null) {
                            tipoEspacioRepository.findAll().stream().findFirst().ifPresent(sub::setTipoEspacio);
                        }
                        espacioFisicoRepository.save(sub);
                        guardado.getEspacios().add(sub);
                    }
                }
            }
        }

        // Documentos de respaldo (MinIO)
        if (request.documentos() != null && !request.documentos().isEmpty()) {
            for (RegistrarInmuebleRequest.DocumentoRespaldoRequest docReq : request.documentos()) {
                InmuebleDocumentacionRespaldoJpaEntity doc = new InmuebleDocumentacionRespaldoJpaEntity();
                doc.setId(UUID.randomUUID());
                doc.setInmueble(guardado);
                doc.setNombreArchivo(docReq.nombreArchivo());
                doc.setUrlMinio(docReq.urlMinio());
                doc.setTipoAnexo(docReq.tipoAnexo());
                doc.setDescripcion(docReq.descripcion());
                doc.setRutaRepositorio("03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa");
                doc.setFechaSubida(Instant.now());
                documentacionRepository.save(doc);
                guardado.getDocumentos().add(doc);
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToInmuebleResponse(guardado));
    }

    @PutMapping("/inmuebles/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
    @Transactional
    public ResponseEntity<InmuebleResponse> actualizarInmueble(
            @PathVariable UUID id,
            @RequestBody RegistrarInmuebleRequest request
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = (auth != null && auth.getAuthorities() != null)
                ? auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet())
                : Collections.emptySet();

        boolean isAdmin = roles.contains("ROLE_AEA");
        boolean isInfra = roles.contains("ROLE_INFRAESTRUCTURA");
        boolean isBedelia = roles.contains("ROLE_BEDELIA");
        boolean isHyS = roles.contains("ROLE_HIGIENE_SEGURIDAD");

        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InmuebleJpaEntity entity = opt.get();

        // 1. Mutar metadatos del inmueble ONLY if isAdmin || isInfra
        if (isAdmin || isInfra) {
            if (request.numeroOrden() != null) entity.setNumeroOrden(request.numeroOrden());
            if (request.nombre() != null) entity.setNombre(request.nombre());
            if (request.domicilio() != null) {
                entity.setDomicilio(request.domicilio());
            } else if (request.calle() != null && request.numero() != null) {
                entity.setDomicilio(request.calle() + " " + request.numero());
            }
            if (request.calle() != null) entity.setCalle(request.calle());
            if (request.numero() != null) entity.setNumero(request.numero());
            if (request.sedeId() != null) {
                sedeRepository.findById(request.sedeId()).ifPresent(entity::setSede);
            }
            if (request.anioConstruccion() != null) entity.setAnioConstruccion(request.anioConstruccion());
            if (request.anioInicioInstitucion() != null) entity.setAnioInicioInstitucion(request.anioInicioInstitucion());
            if (request.propiedadInstitucion() != null) entity.setPropiedadInstitucion(request.propiedadInstitucion());
            if (request.condicionTenencia() != null) entity.setCondicionTenencia(request.condicionTenencia().toUpperCase());
            if (request.situacion() != null) entity.setSituacion(request.situacion());
            if (request.vinculaTodasCarreras() != null) entity.setVinculaTodasCarreras(request.vinculaTodasCarreras());
            if (request.superficieCubiertaM2() != null) entity.setSuperficieCubiertaM2(request.superficieCubiertaM2());
            if (request.superficieTotalM2() != null) entity.setSuperficieTotalM2(request.superficieTotalM2());
            if (request.observaciones() != null) entity.setObservaciones(request.observaciones());
            if (request.estadoConeau() != null) entity.setEstadoConeau(request.estadoConeau());

            if (request.provinciaId() != null) {
                provinciaRepository.findById(request.provinciaId()).ifPresent(entity::setProvincia);
            }
            if (request.departamentoId() != null) {
                departamentoRepository.findById(request.departamentoId()).ifPresent(entity::setDepartamentoGeografico);
            }
            if (request.localidadId() != null) {
                localidadRepository.findById(request.localidadId()).ifPresent(entity::setLocalidad);
            }

            if (request.carrerasIds() != null) {
                Set<CarreraJpaEntity> carreras = new HashSet<>(carreraRepository.findAllById(request.carrerasIds()));
                entity.setCarreras(carreras);
            }
        }
        entity.setActualizadoEn(Instant.now());

        // 2. Espacios físicos y subespacios - Sincronizar
        if (request.espacios() != null) {
            List<EspacioFisicoJpaEntity> actuales = espacioFisicoRepository.findByInmuebleIdAndActivoTrue(entity.getId());
            Set<UUID> idsEnviados = new HashSet<>();
            for (EspacioFisicoRequest espReq : request.espacios()) {
                if (espReq.id() != null) idsEnviados.add(espReq.id());
                if (espReq.subespacios() != null) {
                    for (EspacioFisicoRequest subReq : espReq.subespacios()) {
                        if (subReq.id() != null) idsEnviados.add(subReq.id());
                    }
                }
            }

            // Desactivar los que ya no existen (ONLY isAdmin || isInfra)
            if (isAdmin || isInfra) {
                for (EspacioFisicoJpaEntity act : actuales) {
                    if (!idsEnviados.contains(act.getId())) {
                        act.setActivo(false);
                        espacioFisicoRepository.save(act);
                    }
                }
            }

            // Actualizar o crear padres y subespacios
            for (EspacioFisicoRequest espReq : request.espacios()) {
                // Si el espacio es nuevo (id == null) y el usuario es strictly Bedelía, no se permite crear espacios
                if (espReq.id() == null && !isAdmin && !isInfra) {
                    continue;
                }

                EspacioFisicoJpaEntity esp;
                if (espReq.id() != null) {
                    esp = espacioFisicoRepository.findById(espReq.id()).orElse(new EspacioFisicoJpaEntity());
                    if (esp.getId() == null) {
                        if (!isAdmin && !isInfra) continue; // no crear
                        esp.setId(espReq.id());
                        esp.setCreadoEn(Instant.now());
                    }
                } else {
                    esp = new EspacioFisicoJpaEntity();
                    esp.setId(UUID.randomUUID());
                    esp.setCreadoEn(Instant.now());
                }
                esp.setInmueble(entity);
                esp.setEspacioPadre(null);
                esp.setEsMaestro(entity.getEsMaestro());
                esp.setProcesoId(entity.getProcesoId());

                // Campos de Infraestructura
                if (isAdmin || isInfra) {
                    String espDenom = (espReq.denominacion() != null && !espReq.denominacion().isBlank()) ? espReq.denominacion() :
                            (espReq.descripcionInfraestructura() != null && !espReq.descripcionInfraestructura().isBlank() ? espReq.descripcionInfraestructura() : "Espacio Físico");
                    esp.setDenominacion(espDenom);
                    esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura() != null ? espReq.descripcionInfraestructura() : espDenom);
                    esp.setSuperficieM2(espReq.superficieM2());
                    esp.setEquipamientoDisponible(espReq.equipamientoDisponible());
                    boolean tieneSubs = espReq.tieneSubespacios() != null && espReq.tieneSubespacios();
                    esp.setTieneSubespacios(tieneSubs);
                    if (espReq.estadoConeau() != null) esp.setEstadoConeau(espReq.estadoConeau());
                    esp.setActivo(true);
                    if (espReq.tipoEspacioId() != null) {
                        tipoEspacioRepository.findById(espReq.tipoEspacioId()).ifPresent(esp::setTipoEspacio);
                    }
                    if (esp.getTipoEspacio() == null) {
                        tipoEspacioRepository.findAll().stream().findFirst().ifPresent(esp::setTipoEspacio);
                    }
                }

                // Campos de Bedelía
                if (isAdmin || isBedelia) {
                    if (espReq.cantidad() != null) esp.setCantidad(espReq.cantidad());
                    if (espReq.capacidadPersonasSentadas() != null) {
                        esp.setCapacidadPersonasSentadas(espReq.capacidadPersonasSentadas());
                    }
                    if (espReq.capacidadEstudiantes() != null) {
                        esp.setCapacidadEstudiantes(espReq.capacidadEstudiantes());
                    } else if (espReq.capacidadPersonasSentadas() != null && esp.getCapacidadEstudiantes() == 0) {
                        esp.setCapacidadEstudiantes(espReq.capacidadPersonasSentadas());
                    }
                }

                // Campo Higiene y Seguridad: Solo se modifica si isAdmin || isHyS. Si no, se preserva el valor existente.
                if (isAdmin || isHyS) {
                    if (espReq.observacionHigieneSeguridad() != null) {
                        esp.setObservacionHigieneSeguridad(espReq.observacionHigieneSeguridad());
                    }
                }

                espacioFisicoRepository.save(esp);

                // Subespacios hijos
                boolean tieneSubsActual = Boolean.TRUE.equals(esp.getTieneSubespacios());
                if (tieneSubsActual && espReq.subespacios() != null) {
                    for (EspacioFisicoRequest subReq : espReq.subespacios()) {
                        if (subReq.id() == null && !isAdmin && !isInfra) {
                            continue;
                        }

                        EspacioFisicoJpaEntity sub;
                        if (subReq.id() != null) {
                            sub = espacioFisicoRepository.findById(subReq.id()).orElse(new EspacioFisicoJpaEntity());
                            if (sub.getId() == null) {
                                if (!isAdmin && !isInfra) continue;
                                sub.setId(subReq.id());
                                sub.setCreadoEn(Instant.now());
                            }
                        } else {
                            sub = new EspacioFisicoJpaEntity();
                            sub.setId(UUID.randomUUID());
                            sub.setCreadoEn(Instant.now());
                        }
                        sub.setInmueble(entity);
                        sub.setEspacioPadre(esp);
                        sub.setTieneSubespacios(false);
                        sub.setEsMaestro(entity.getEsMaestro());
                        sub.setProcesoId(entity.getProcesoId());

                        if (isAdmin || isInfra) {
                            String subDenom = (subReq.denominacion() != null && !subReq.denominacion().isBlank()) ? subReq.denominacion() :
                                    (subReq.descripcionInfraestructura() != null && !subReq.descripcionInfraestructura().isBlank() ? subReq.descripcionInfraestructura() : "Subespacio");
                            sub.setDenominacion(subDenom);
                            sub.setDescripcionInfraestructura(subReq.descripcionInfraestructura() != null ? subReq.descripcionInfraestructura() : subDenom);
                            sub.setSuperficieM2(subReq.superficieM2());
                            sub.setEquipamientoDisponible(subReq.equipamientoDisponible());
                            if (subReq.estadoConeau() != null) sub.setEstadoConeau(subReq.estadoConeau());
                            sub.setActivo(true);
                            if (subReq.tipoEspacioId() != null) {
                                tipoEspacioRepository.findById(subReq.tipoEspacioId()).ifPresent(sub::setTipoEspacio);
                            }
                            if (sub.getTipoEspacio() == null) {
                                sub.setTipoEspacio(esp.getTipoEspacio());
                            }
                            if (sub.getTipoEspacio() == null) {
                                tipoEspacioRepository.findAll().stream().findFirst().ifPresent(sub::setTipoEspacio);
                            }
                        }

                        if (isAdmin || isBedelia) {
                            if (subReq.cantidad() != null) sub.setCantidad(subReq.cantidad());
                            if (subReq.capacidadPersonasSentadas() != null) {
                                sub.setCapacidadPersonasSentadas(subReq.capacidadPersonasSentadas());
                            }
                            if (subReq.capacidadEstudiantes() != null) {
                                sub.setCapacidadEstudiantes(subReq.capacidadEstudiantes());
                            } else if (subReq.capacidadPersonasSentadas() != null && sub.getCapacidadEstudiantes() == 0) {
                                sub.setCapacidadEstudiantes(subReq.capacidadPersonasSentadas());
                            }
                        }

                        if (isAdmin || isHyS) {
                            if (subReq.observacionHigieneSeguridad() != null) {
                                sub.setObservacionHigieneSeguridad(subReq.observacionHigieneSeguridad());
                            }
                        }

                        espacioFisicoRepository.save(sub);
                    }
                }
            }
        }

        // Documentos de respaldo (Adjuntar Documentos en Actualizar Inmueble) - Solo Admin o Infra
        if ((isAdmin || isInfra) && request.documentos() != null && !request.documentos().isEmpty()) {
            List<InmuebleDocumentacionRespaldoJpaEntity> docsExistentes = documentacionRepository.findByInmuebleIdOrderByFechaSubidaDesc(entity.getId());
            Set<String> nombresExistentes = docsExistentes.stream()
                    .map(InmuebleDocumentacionRespaldoJpaEntity::getNombreArchivo)
                    .collect(Collectors.toSet());

            for (RegistrarInmuebleRequest.DocumentoRespaldoRequest docReq : request.documentos()) {
                if (!nombresExistentes.contains(docReq.nombreArchivo())) {
                    InmuebleDocumentacionRespaldoJpaEntity doc = new InmuebleDocumentacionRespaldoJpaEntity();
                    doc.setId(UUID.randomUUID());
                    doc.setInmueble(entity);
                    doc.setNombreArchivo(docReq.nombreArchivo());
                    doc.setUrlMinio(docReq.urlMinio());
                    doc.setTipoAnexo(docReq.tipoAnexo());
                    doc.setDescripcion(docReq.descripcion());
                    doc.setRutaRepositorio("03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Normativa");
                    doc.setFechaSubida(Instant.now());
                    documentacionRepository.save(doc);
                }
            }
        }

        InmuebleJpaEntity guardado = inmuebleRepository.save(entity);
        return inmuebleRepository.findByIdCompleto(guardado.getId())
                .map(this::mapToInmuebleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(mapToInmuebleResponse(guardado)));
    }

    @PostMapping("/inmuebles/{id}/verificar")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<InmuebleResponse> verificarYRemitirADea(@PathVariable UUID id) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InmuebleJpaEntity entity = opt.get();
        entity.setVerificado(true);
        entity.setFechaVerificacion(Instant.now());
        entity.setActualizadoEn(Instant.now());
        inmuebleRepository.save(entity);

        return ResponseEntity.ok(mapToInmuebleResponse(entity));
    }

    @DeleteMapping("/inmuebles/{id}")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<Void> eliminarInmueble(@PathVariable UUID id) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        InmuebleJpaEntity entity = opt.get();
        entity.setActivo(false);
        inmuebleRepository.save(entity);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Higiene y Seguridad en Espacios (Exclusivo Rol HIGIENE_SEGURIDAD)
    // -------------------------------------------------------------------------

    @PatchMapping("/espacios/{id}/higiene-seguridad")
    @PreAuthorize("hasAnyRole('AEA', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<EspacioFisicoResponse> actualizarHigieneSeguridad(
            @PathVariable UUID id,
            @RequestBody ActualizarHigieneSeguridadRequest request
    ) {
        EspacioFisicoJpaEntity esp = espacioFisicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio físico no encontrado"));
        esp.setObservacionHigieneSeguridad(request.observacionHigieneSeguridad());
        EspacioFisicoJpaEntity guardado = espacioFisicoRepository.save(esp);
        return ResponseEntity.ok(mapSingleEspacioResponse(guardado, Collections.emptyList()));
    }

    // -------------------------------------------------------------------------
    // Consulta Dinámica de Espacios por Metros Cuadrados (m²)
    // -------------------------------------------------------------------------

    @GetMapping("/espacios/consulta")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<ConsultaEspaciosResponse> consultarEspacios(
            @RequestParam(required = false) UUID procesoId,
            @RequestParam(required = false) UUID tipoEspacioId,
            @RequestParam(required = false) BigDecimal m2Min,
            @RequestParam(required = false) BigDecimal m2Max,
            @RequestParam(required = false) UUID sedeId
    ) {
        List<EspacioFisicoJpaEntity> lista = (procesoId != null)
                ? espacioFisicoRepository.consultarPorProcesoYFiltros(procesoId, tipoEspacioId, m2Min, m2Max, sedeId)
                : espacioFisicoRepository.consultarMaestrosPorFiltros(tipoEspacioId, m2Min, m2Max, sedeId);
        int totalAulas = 0;
        int capacidadTotal = 0;
        BigDecimal totalM2 = BigDecimal.ZERO;
        List<ConsultaEspaciosResponse.EspacioFisicoConsultaItem> items = new ArrayList<>();

        for (EspacioFisicoJpaEntity e : lista) {
            int cant = e.getCantidad() != null ? e.getCantidad() : 1;
            int cap = e.getCapacidadPersonasSentadas() != null ? e.getCapacidadPersonasSentadas() :
                    (e.getCapacidadEstudiantes() != null ? e.getCapacidadEstudiantes() : 0);
            totalAulas += cant;
            capacidadTotal += (cap * cant);
            if (e.getSuperficieM2() != null) {
                totalM2 = totalM2.add(e.getSuperficieM2().multiply(BigDecimal.valueOf(cant)));
            }

            items.add(new ConsultaEspaciosResponse.EspacioFisicoConsultaItem(
                    e.getId(),
                    e.getInmueble() != null ? e.getInmueble().getId() : null,
                    e.getInmueble() != null ? e.getInmueble().getNombre() : "-",
                    e.getInmueble() != null && e.getInmueble().getSede() != null ? e.getInmueble().getSede().getId() : null,
                    e.getInmueble() != null && e.getInmueble().getSede() != null ? e.getInmueble().getSede().getNombre() : "-",
                    e.getTipoEspacio() != null ? e.getTipoEspacio().getId() : null,
                    e.getTipoEspacio() != null ? e.getTipoEspacio().getDenominacion() : "-",
                    e.getDenominacion(),
                    e.getDescripcionInfraestructura(),
                    cant,
                    cap,
                    e.getSuperficieM2(),
                    e.getObservacionHigieneSeguridad(),
                    e.getEspacioPadre() != null,
                    e.getEspacioPadre() != null ? e.getEspacioPadre().getId() : null,
                    e.getEspacioPadre() != null ? e.getEspacioPadre().getDenominacion() : null
            ));
        }

        return ResponseEntity.ok(new ConsultaEspaciosResponse(totalAulas, capacidadTotal, totalM2, items));
    }

    public ResponseEntity<ConsultaEspaciosResponse> consultarEspacios(
            UUID tipoEspacioId,
            BigDecimal m2Min,
            BigDecimal m2Max,
            UUID sedeId
    ) {
        return consultarEspacios(null, tipoEspacioId, m2Min, m2Max, sedeId);
    }

    // -------------------------------------------------------------------------
    // Gestión por Proceso de Acreditación (Snapshot + Plan de Trabajo)
    // -------------------------------------------------------------------------

    @GetMapping("/procesos/{procesoId}/inmuebles")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<?> obtenerInmueblesProceso(
            @PathVariable UUID procesoId,
            @RequestParam(required = false) UUID convocatoriaId,
            @RequestParam(required = false) UUID carreraId
    ) {
        try {
            String claveContexto = construirClaveContexto(procesoId, convocatoriaId, carreraId);
            List<InmuebleDocumentacionRespaldoJpaEntity> copias = documentacionRepository != null
                    ? documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(claveContexto)
                    : Collections.emptyList();
            boolean verificadoCarrera = !copias.isEmpty();
            Instant fechaVerif = verificadoCarrera ? copias.get(0).getFechaSubida() : null;

            List<InmuebleJpaEntity> copiasProceso = inmuebleRepository.findByProcesoIdActivos(procesoId);
            List<InmuebleResponse> response = copiasProceso.stream()
                    .map(c -> mapToInmuebleResponseConEstado(c, verificadoCarrera, fechaVerif))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cargar inmuebles para el proceso " + procesoId, "mensaje", e.getMessage()));
        }
    }

    @GetMapping("/procesos/{procesoId}/infraestructura/estado")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> obtenerEstadoInfraestructuraCarrera(
            @PathVariable UUID procesoId,
            @RequestParam(required = false) UUID convocatoriaId,
            @RequestParam(required = false) UUID carreraId
    ) {
        String claveContexto = construirClaveContexto(procesoId, convocatoriaId, carreraId);
        String rutaCarpeta = construirRutaCarpetaCarrera(procesoId, convocatoriaId, carreraId);
        String nombreArchivo = construirNombreArchivoCarrera(procesoId, carreraId);

        List<InmuebleDocumentacionRespaldoJpaEntity> copias = documentacionRepository != null
                ? documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(claveContexto)
                : Collections.emptyList();

        boolean verificado = !copias.isEmpty();
        Instant fechaVerificacion = verificado ? copias.get(0).getFechaSubida() : null;
        String rutaAlmacenada = verificado ? copias.get(0).getRutaRepositorio() + "/" + copias.get(0).getNombreArchivo() : (rutaCarpeta + "/" + nombreArchivo);

        Map<String, Object> resp = new HashMap<>();
        resp.put("procesoId", procesoId);
        resp.put("verificado", verificado);
        resp.put("rutaCarpeta", verificado ? copias.get(0).getRutaRepositorio() : rutaCarpeta);
        resp.put("nombreArchivo", verificado ? copias.get(0).getNombreArchivo() : nombreArchivo);
        resp.put("rutaCompleta", rutaAlmacenada);
        resp.put("fechaVerificacion", fechaVerificacion);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/procesos/{procesoId}/inmuebles/copiar-maestro")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<?> forzarCopiaMaestro(@PathVariable UUID procesoId) {
        try {
            List<InmuebleJpaEntity> nuevos = clonarInmueblesMaestrosParaProceso(procesoId);
            List<InmuebleResponse> response = nuevos.stream()
                    .map(this::mapToInmuebleResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al sincronizar padrón maestro para el proceso " + procesoId, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/procesos/{procesoId}/infraestructura/extender-plazo")
    @PreAuthorize("hasRole('AEA')")
    @Transactional
    public ResponseEntity<?> extenderPlazoInfraestructura(
            @PathVariable UUID procesoId,
            @RequestBody ExtenderPlazoInfraestructuraRequest request
    ) {
        if (request.nuevaFechaLimite() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "La nueva fecha límite es obligatoria"));
        }

        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PlanTrabajo plan = planOpt.get();
        Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                .filter(a -> "4.1".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("infraestructura"))
                .findFirst();

        if (actOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se encontró la Actividad 4.1 de Infraestructura en el Plan de Trabajo"));
        }

        ActividadPlanTrabajo act = actOpt.get();
        act.agregarPlazoExtendido(request.nuevaFechaLimite());
        planTrabajoRepository.saveActividad(act);
        planTrabajoRepository.save(plan);

        Map<String, Object> resp = new HashMap<>();
        resp.put("procesoId", procesoId);
        resp.put("codigoActividad", act.getCodigoActividad());
        resp.put("nuevaFechaLimite", act.getUltimoPlazoVigente());
        resp.put("plazosExtendidos", act.getPlazosExtendidos());
        resp.put("motivo", request.motivo());
        resp.put("mensaje", "Plazo extendido exitosamente y sincronizado con el Plan de Trabajo (hoja INFO)");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/procesos/{procesoId}/infraestructura/verificar-y-remitir")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<VerificarInfraestructuraResponse> verificarYRemitirInfraestructura(
            @PathVariable UUID procesoId,
            @RequestParam(required = false) UUID convocatoriaId,
            @RequestParam(required = false) UUID carreraId
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        Instant now = Instant.now();

        List<InmuebleJpaEntity> copiasProceso = inmuebleRepository.findByProcesoIdActivos(procesoId);
        List<InmuebleJpaEntity> fuente = !copiasProceso.isEmpty()
                ? copiasProceso
                : inmuebleRepository.findMaestrosActivos();
        String claveContexto = construirClaveContexto(procesoId, convocatoriaId, carreraId);
        String rutaCarpeta = construirRutaCarpetaCarrera(procesoId, convocatoriaId, carreraId);
        String nombreArchivo = construirNombreArchivoCarrera(procesoId, carreraId);
        String rutaCompletaArchivo = rutaCarpeta + "/" + nombreArchivo;

        // Generar y almacenar el archivo RG-005 en la carpeta de la carrera para esta convocatoria
        String urlAlmacenada = "/minio/acreditacion/" + rutaCompletaArchivo;
        try {
            List<InmuebleResponse> snapshotDtos = fuente.stream()
                    .map(m -> mapToInmuebleResponseConEstado(m, true, now))
                    .collect(Collectors.toList());
            byte[] jsonBytes;
            if (objectMapper != null) {
                jsonBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(snapshotDtos);
            } else {
                jsonBytes = snapshotDtos.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
            if (documentStoragePort != null) {
                String storedKey = documentStoragePort.uploadFile(jsonBytes, rutaCompletaArchivo, "application/json");
                String fileUrl = documentStoragePort.getFileUrl(storedKey);
                if (fileUrl != null && !fileUrl.isBlank()) {
                    urlAlmacenada = fileUrl;
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo subir snapshot '{}' a almacenamiento externo; se conserva ruta lógica: {}", rutaCompletaArchivo, ex.getMessage());
        }

        if (!fuente.isEmpty() && documentacionRepository != null) {
            List<InmuebleDocumentacionRespaldoJpaEntity> previas = documentacionRepository.findByTipoAnexoOrderByFechaSubidaDesc(claveContexto);
            InmuebleDocumentacionRespaldoJpaEntity docSnapshot = previas.isEmpty()
                    ? new InmuebleDocumentacionRespaldoJpaEntity()
                    : previas.get(0);
            if (docSnapshot.getId() == null) {
                docSnapshot.setId(UUID.randomUUID());
            }
            docSnapshot.setInmueble(fuente.get(0));
            docSnapshot.setNombreArchivo(nombreArchivo);
            docSnapshot.setRutaRepositorio(rutaCarpeta);
            docSnapshot.setUrlMinio(urlAlmacenada);
            docSnapshot.setTipoAnexo(claveContexto);
            docSnapshot.setDescripcion("Copia confirmada del padrón general de inmuebles (RG-005) en la carpeta de la carrera");
            docSnapshot.setFechaSubida(now);
            documentacionRepository.save(docSnapshot);
        }

        String estadoActividad = "COMPLETADA";
        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                    .filter(a -> "4.1".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("infraestructura"))
                    .findFirst();

            if (actOpt.isPresent()) {
                ActividadPlanTrabajo act = actOpt.get();
                act.completar(usuarioId, "Verificación de inmuebles completada y archivo copiado a " + rutaCompletaArchivo);
                planTrabajoRepository.saveActividad(act);
                planTrabajoRepository.save(plan);
                estadoActividad = act.getEstado().name();
            }
        }

        return ResponseEntity.ok(new VerificarInfraestructuraResponse(
                procesoId,
                true,
                estadoActividad,
                now,
                "Infraestructura confirmada. Archivo copiado a la carpeta de la carrera: " + rutaCompletaArchivo
        ));
    }

    // Sobrecarga sin query params para compatibilidad directa en tests unitarios
    public ResponseEntity<VerificarInfraestructuraResponse> verificarYRemitirInfraestructura(UUID procesoId) {
        return verificarYRemitirInfraestructura(procesoId, null, null);
    }

    public ResponseEntity<?> obtenerInmueblesProceso(UUID procesoId) {
        return obtenerInmueblesProceso(procesoId, null, null);
    }

    // -------------------------------------------------------------------------
    // Tipos de Espacio Físico
    // -------------------------------------------------------------------------

    @GetMapping("/tipos-espacio")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD', 'DIRECTOR_CARRERA', 'CAREER_DIRECTOR', 'COMISION_AUTOEVALUACION', 'DOCENTE')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TipoEspacioResponse>> listarTiposEspacio() {
        List<TipoEspacioResponse> response = tipoEspacioRepository.findByActivoTrueOrderByDenominacionAsc().stream()
                .map(t -> new TipoEspacioResponse(t.getId(), t.getCodigo(), t.getDenominacion(), t.getActivo()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tipos-espacio")
    @PreAuthorize("hasAnyRole('AEA', 'INFRAESTRUCTURA')")
    @Transactional
    public ResponseEntity<TipoEspacioResponse> crearTipoEspacio(@RequestBody CrearTipoEspacioRequest request) {
        if (request.denominacion() == null || request.denominacion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La denominación del tipo de espacio es requerida");
        }

        String codigo = (request.codigo() != null && !request.codigo().isBlank())
                ? request.codigo().toUpperCase().replace(" ", "_")
                : request.denominacion().toUpperCase().replace(" ", "_");

        TipoEspacioFisicoJpaEntity tipo = new TipoEspacioFisicoJpaEntity();
        tipo.setId(UUID.randomUUID());
        tipo.setCodigo(codigo);
        tipo.setDenominacion(request.denominacion());
        tipo.setActivo(true);
        tipo.setCreadoEn(Instant.now());

        TipoEspacioFisicoJpaEntity guardado = tipoEspacioRepository.save(tipo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TipoEspacioResponse(guardado.getId(), guardado.getCodigo(), guardado.getDenominacion(), guardado.getActivo()));
    }

    // -------------------------------------------------------------------------
    // Mappers y Helpers
    // -------------------------------------------------------------------------

    private String construirClaveContexto(UUID procesoId, UUID convocatoriaId, UUID carreraId) {
        if (convocatoriaId != null && carreraId != null) {
            return "SNAPSHOT_RG005:" + convocatoriaId + "_" + carreraId;
        }
        if (carreraId != null) {
            return "SNAPSHOT_RG005:" + procesoId + "_" + carreraId;
        }
        return "SNAPSHOT_RG005:" + procesoId;
    }

    private String construirRutaCarpetaCarrera(UUID procesoId, UUID convocatoriaId, UUID carreraId) {
        String convSegmento = convocatoriaId != null ? convocatoriaId.toString() : procesoId.toString();
        String carreraSegmento = resolverSegmentoCarrera(procesoId, carreraId);
        return "03-Autoevaluación/03_01-Infraestructura equipamiento y biblioteca/Convocatorias/"
                + convSegmento + "/Carrera_" + carreraSegmento;
    }

    private String construirNombreArchivoCarrera(UUID procesoId, UUID carreraId) {
        String carreraSegmento = resolverSegmentoCarrera(procesoId, carreraId);
        return "RG-005_Registro_Inmuebles_" + carreraSegmento + ".json";
    }

    private String resolverSegmentoCarrera(UUID procesoId, UUID carreraId) {
        if (carreraId != null && carreraRepository != null) {
            return carreraRepository.findById(carreraId)
                    .map(c -> (c.getSigla() != null && !c.getSigla().isBlank()) ? c.getSigla() : c.getId().toString())
                    .orElse(carreraId.toString());
        }
        return procesoId != null ? procesoId.toString() : "General";
    }

    private InmuebleResponse mapToInmuebleResponse(InmuebleJpaEntity entity) {
        boolean verif = !Boolean.TRUE.equals(entity.getEsMaestro()) && Boolean.TRUE.equals(entity.getVerificado());
        Instant fechaVerif = Boolean.TRUE.equals(entity.getEsMaestro()) ? null : entity.getFechaVerificacion();
        return mapToInmuebleResponseConEstado(entity, verif, fechaVerif);
    }

    private InmuebleResponse mapToInmuebleResponseConEstado(InmuebleJpaEntity entity, boolean verificadoEfectivo, Instant fechaVerificacionEfectiva) {
        List<InmuebleResponse.CarreraVinculadaResponse> carreras = (entity.getCarreras() == null) ? Collections.emptyList() :
                entity.getCarreras().stream()
                        .map(c -> new InmuebleResponse.CarreraVinculadaResponse(c.getId(), c.getNombre(), c.getSigla()))
                        .collect(Collectors.toList());

        List<EspacioFisicoResponse> espacios = Collections.emptyList();
        if (entity.getEspacios() != null) {
            List<EspacioFisicoJpaEntity> todosActivos = entity.getEspacios().stream()
                    .filter(EspacioFisicoJpaEntity::getActivo)
                    .toList();

            // Mapear solo espacios padre en primer nivel, con sus subespacios anidados
            espacios = todosActivos.stream()
                    .filter(e -> e.getEspacioPadre() == null)
                    .map(padre -> {
                        List<EspacioFisicoResponse> subespacios = todosActivos.stream()
                                .filter(c -> c.getEspacioPadre() != null && padre.getId().equals(c.getEspacioPadre().getId()))
                                .map(hijo -> mapSingleEspacioResponse(hijo, Collections.emptyList()))
                                .toList();
                        return mapSingleEspacioResponse(padre, subespacios);
                    })
                    .collect(Collectors.toList());
        }

        List<DocumentacionRespaldoResponse> documentos = (entity.getDocumentos() == null) ? Collections.emptyList() :
                entity.getDocumentos().stream()
                        .filter(d -> d.getTipoAnexo() == null || !d.getTipoAnexo().startsWith("SNAPSHOT_RG005:"))
                        .map(d -> new DocumentacionRespaldoResponse(
                                d.getId(),
                                d.getNombreArchivo(),
                                d.getUrlMinio(),
                                d.getRutaRepositorio(),
                                d.getTipoAnexo(),
                                d.getDescripcion(),
                                d.getSubidoPorUsuario() != null ? d.getSubidoPorUsuario().getUsername() : null,
                                d.getFechaSubida()
                        ))
                        .collect(Collectors.toList());

        UUID sedeId = entity.getSede() != null ? entity.getSede().getId() : null;
        String sedeNombre = entity.getSede() != null ? entity.getSede().getNombre() : null;
        String uaDenominacion = entity.getSede() != null && entity.getSede().getUnidadAcademica() != null ?
                entity.getSede().getUnidadAcademica().getDenominacion() : null;

        return new InmuebleResponse(
                entity.getId(),
                entity.getInstitucion() != null ? entity.getInstitucion().getId() : null,
                entity.getNumeroOrden(),
                entity.getNombre(),
                entity.getDomicilio(),
                entity.getCalle(),
                entity.getNumero(),
                sedeId,
                sedeNombre,
                uaDenominacion,
                entity.getProvincia() != null ? entity.getProvincia().getId() : null,
                entity.getProvincia() != null ? entity.getProvincia().getNombre() : null,
                entity.getDepartamentoGeografico() != null ? entity.getDepartamentoGeografico().getId() : null,
                entity.getDepartamentoGeografico() != null ? entity.getDepartamentoGeografico().getNombre() : null,
                entity.getLocalidad() != null ? entity.getLocalidad().getId() : null,
                entity.getLocalidad() != null ? entity.getLocalidad().getNombre() : null,
                entity.getAnioConstruccion(),
                entity.getAnioInicioInstitucion(),
                entity.getPropiedadInstitucion(),
                entity.getCondicionTenencia(),
                entity.getSituacion(),
                entity.getVinculaTodasCarreras(),
                entity.getEsMaestro(),
                entity.getProcesoId(),
                entity.getSuperficieCubiertaM2(),
                entity.getSuperficieTotalM2(),
                entity.getObservaciones(),
                verificadoEfectivo,
                fechaVerificacionEfectiva,
                verificadoEfectivo ? "VERIFICADO" : "CARGADO",
                entity.getActivo(),
                carreras,
                espacios,
                documentos,
                espacios.size(),
                !documentos.isEmpty()
        );
    }

    private EspacioFisicoResponse mapSingleEspacioResponse(EspacioFisicoJpaEntity e, List<EspacioFisicoResponse> subespacios) {
        return new EspacioFisicoResponse(
                e.getId(),
                e.getTipoEspacio() != null ? e.getTipoEspacio().getId() : null,
                e.getTipoEspacio() != null ? e.getTipoEspacio().getCodigo() : null,
                e.getTipoEspacio() != null ? e.getTipoEspacio().getDenominacion() : null,
                e.getDenominacion(),
                e.getDescripcionInfraestructura(),
                e.getCantidad() != null ? e.getCantidad() : 1,
                e.getCapacidadPersonasSentadas() != null ? e.getCapacidadPersonasSentadas() : e.getCapacidadEstudiantes(),
                e.getCapacidadEstudiantes(),
                e.getSuperficieM2(),
                e.getEquipamientoDisponible(),
                e.getEstadoConeau(),
                e.getActivo(),
                e.getEspacioPadre() != null ? e.getEspacioPadre().getId() : null,
                e.getTieneSubespacios() != null ? e.getTieneSubespacios() : false,
                e.getObservacionHigieneSeguridad(),
                subespacios
        );
    }

    private List<InmuebleJpaEntity> clonarInmueblesMaestrosParaProceso(UUID procesoId) {
        List<InmuebleJpaEntity> maestros = inmuebleRepository.findMaestrosActivos();
        List<InmuebleJpaEntity> clonados = new ArrayList<>();

        for (InmuebleJpaEntity m : maestros) {
            InmuebleJpaEntity c = new InmuebleJpaEntity();
            c.setId(UUID.randomUUID());
            c.setInstitucion(m.getInstitucion());
            c.setNumeroOrden(m.getNumeroOrden());
            c.setNombre(m.getNombre());
            c.setDomicilio(m.getDomicilio());
            c.setCalle(m.getCalle());
            c.setNumero(m.getNumero());
            c.setSede(m.getSede());
            c.setProvincia(m.getProvincia());
            c.setDepartamentoGeografico(m.getDepartamentoGeografico());
            c.setLocalidad(m.getLocalidad());
            c.setAnioConstruccion(m.getAnioConstruccion());
            c.setAnioInicioInstitucion(m.getAnioInicioInstitucion());
            c.setPropiedadInstitucion(m.getPropiedadInstitucion());
            c.setCondicionTenencia(m.getCondicionTenencia());
            c.setSituacion(m.getSituacion());
            c.setVinculaTodasCarreras(m.getVinculaTodasCarreras());
            c.setEsMaestro(false);
            c.setProcesoId(procesoId);
            c.setOrigenMaestroId(m.getId());
            c.setSuperficieCubiertaM2(m.getSuperficieCubiertaM2());
            c.setSuperficieTotalM2(m.getSuperficieTotalM2());
            c.setObservaciones(m.getObservaciones());
            c.setVerificado(false);
            c.setFechaVerificacion(null);
            c.setEstadoConeau("CARGADO");
            c.setActivo(true);
            c.setCreadoEn(Instant.now());
            c.setActualizadoEn(Instant.now());

            if (m.getCarreras() != null && !m.getCarreras().isEmpty()) {
                c.setCarreras(new HashSet<>(m.getCarreras()));
            }

            InmuebleJpaEntity inmGuardado = inmuebleRepository.save(c);

            // Clonar espacios físicos maestros preservando jerarquía de subespacios
            if (m.getEspacios() != null) {
                Map<UUID, EspacioFisicoJpaEntity> mapaPadres = new HashMap<>();
                for (EspacioFisicoJpaEntity espM : m.getEspacios()) {
                    if (espM.getActivo() && espM.getEspacioPadre() == null) {
                        EspacioFisicoJpaEntity espC = new EspacioFisicoJpaEntity();
                        espC.setId(UUID.randomUUID());
                        espC.setInmueble(inmGuardado);
                        espC.setTipoEspacio(espM.getTipoEspacio());
                        espC.setDenominacion(espM.getDenominacion());
                        espC.setDescripcionInfraestructura(espM.getDescripcionInfraestructura());
                        espC.setCantidad(espM.getCantidad());
                        espC.setCapacidadPersonasSentadas(espM.getCapacidadPersonasSentadas());
                        espC.setCapacidadEstudiantes(espM.getCapacidadEstudiantes());
                        espC.setSuperficieM2(espM.getSuperficieM2());
                        espC.setEquipamientoDisponible(espM.getEquipamientoDisponible());
                        espC.setObservacionHigieneSeguridad(espM.getObservacionHigieneSeguridad());
                        espC.setTieneSubespacios(espM.getTieneSubespacios());
                        espC.setEstadoConeau("CARGADO");
                        espC.setEsMaestro(false);
                        espC.setProcesoId(procesoId);
                        espC.setOrigenMaestroId(espM.getId());
                        espC.setActivo(true);
                        espC.setCreadoEn(Instant.now());
                        espacioFisicoRepository.save(espC);
                        inmGuardado.getEspacios().add(espC);
                        mapaPadres.put(espM.getId(), espC);
                    }
                }

                // Clonar hijos vinculados
                for (EspacioFisicoJpaEntity espM : m.getEspacios()) {
                    if (espM.getActivo() && espM.getEspacioPadre() != null) {
                        EspacioFisicoJpaEntity padreClonado = mapaPadres.get(espM.getEspacioPadre().getId());
                        EspacioFisicoJpaEntity espC = new EspacioFisicoJpaEntity();
                        espC.setId(UUID.randomUUID());
                        espC.setInmueble(inmGuardado);
                        espC.setEspacioPadre(padreClonado);
                        espC.setTipoEspacio(espM.getTipoEspacio());
                        espC.setDenominacion(espM.getDenominacion());
                        espC.setDescripcionInfraestructura(espM.getDescripcionInfraestructura());
                        espC.setCantidad(espM.getCantidad());
                        espC.setCapacidadPersonasSentadas(espM.getCapacidadPersonasSentadas());
                        espC.setCapacidadEstudiantes(espM.getCapacidadEstudiantes());
                        espC.setSuperficieM2(espM.getSuperficieM2());
                        espC.setEquipamientoDisponible(espM.getEquipamientoDisponible());
                        espC.setObservacionHigieneSeguridad(espM.getObservacionHigieneSeguridad());
                        espC.setTieneSubespacios(false);
                        espC.setEstadoConeau("CARGADO");
                        espC.setEsMaestro(false);
                        espC.setProcesoId(procesoId);
                        espC.setOrigenMaestroId(espM.getId());
                        espC.setActivo(true);
                        espC.setCreadoEn(Instant.now());
                        espacioFisicoRepository.save(espC);
                        inmGuardado.getEspacios().add(espC);
                    }
                }
            }

            clonados.add(inmGuardado);
        }

        return clonados;
    }

    private UUID resolverUsuarioAutenticadoId() {
        if (authenticatedUserProvider != null) {
            return authenticatedUserProvider.getAuthenticatedUserIdOrFallback();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            String principal = auth.getPrincipal().toString();
            Optional<User> userOpt = userRepository.findByEmail(principal);
            if (userOpt.isPresent()) {
                return userOpt.get().getId();
            }
        }
        return userRepository.findByEmail("admin@undec.edu.ar")
                .map(User::getId)
                .orElse(UUID.fromString("b915837d-6a68-4ad1-95b9-5f6455e586ce"));
    }
}
