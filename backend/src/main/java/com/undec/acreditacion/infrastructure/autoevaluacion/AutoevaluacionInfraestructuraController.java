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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmuebles(
            @RequestParam(name = "esMaestro", required = false) Boolean esMaestro
    ) {
        List<InmuebleJpaEntity> entidades;
        if (Boolean.TRUE.equals(esMaestro)) {
            entidades = inmuebleRepository.findMaestrosActivos();
        } else {
            entidades = inmuebleRepository.findAllActivosConRelaciones();
        }
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/maestro")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InmuebleResponse>> listarInmueblesMaestros() {
        List<InmuebleJpaEntity> entidades = inmuebleRepository.findMaestrosActivos();
        List<InmuebleResponse> response = entidades.stream()
                .map(this::mapToInmuebleResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inmuebles/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<InmuebleResponse> obtenerInmueble(@PathVariable UUID id) {
        return inmuebleRepository.findByIdCompleto(id)
                .map(this::mapToInmuebleResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/inmuebles")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
        // Si no se asigna a un proceso particular, pertenece siempre al padrón maestro para no quedar huérfano
        boolean esMaestro = (request.procesoId() == null) || Boolean.TRUE.equals(request.esMaestro());
        entity.setEsMaestro(esMaestro);
        entity.setProcesoId(request.procesoId());
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
        if (request.departamentoId() != null) {
            departamentoRepository.findById(request.departamentoId()).ifPresent(entity::setDepartamentoGeografico);
        }
        if (request.localidadId() != null) {
            localidadRepository.findById(request.localidadId()).ifPresent(entity::setLocalidad);
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
                esp.setDenominacion(espReq.denominacion());
                esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura() != null ? espReq.descripcionInfraestructura() : espReq.denominacion());
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
                        sub.setDenominacion(subReq.denominacion());
                        sub.setDescripcionInfraestructura(subReq.descripcionInfraestructura() != null ? subReq.descripcionInfraestructura() : subReq.denominacion());
                        sub.setCantidad(subReq.cantidad() != null ? subReq.cantidad() : 1);
                        int subCap = subReq.capacidadPersonasSentadas() != null ? subReq.capacidadPersonasSentadas() :
                                (subReq.capacidadEstudiantes() != null ? subReq.capacidadEstudiantes() : 0);
                        sub.setCapacidadPersonasSentadas(subCap);
                        sub.setCapacidadEstudiantes(subCap);
                        sub.setSuperficieM2(subReq.superficieM2());
                        sub.setEquipamientoDisponible(subReq.equipamientoDisponible());
                        sub.setObservacionHigieneSeguridad(subReq.observacionHigieneSeguridad());
                        sub.setEstadoConeau(subReq.estadoConeau() != null ? subReq.estadoConeau() : "CARGADO");
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
    @Transactional
    public ResponseEntity<InmuebleResponse> actualizarInmueble(
            @PathVariable UUID id,
            @RequestBody RegistrarInmuebleRequest request
    ) {
        Optional<InmuebleJpaEntity> opt = inmuebleRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InmuebleJpaEntity entity = opt.get();
        if (request.numeroOrden() != null) entity.setNumeroOrden(request.numeroOrden());
        entity.setNombre(request.nombre());
        entity.setDomicilio(request.domicilio() != null ? request.domicilio() : (request.calle() + " " + request.numero()));
        if (request.calle() != null) entity.setCalle(request.calle());
        if (request.numero() != null) entity.setNumero(request.numero());
        if (request.sedeId() != null) {
            sedeRepository.findById(request.sedeId()).ifPresent(entity::setSede);
        }
        if (request.anioConstruccion() != null) entity.setAnioConstruccion(request.anioConstruccion());
        if (request.anioInicioInstitucion() != null) entity.setAnioInicioInstitucion(request.anioInicioInstitucion());
        entity.setPropiedadInstitucion(request.propiedadInstitucion() != null ? request.propiedadInstitucion() : true);
        entity.setCondicionTenencia(request.condicionTenencia() != null ? request.condicionTenencia().toUpperCase() : "PROPIO");
        if (request.situacion() != null) entity.setSituacion(request.situacion());
        if (request.vinculaTodasCarreras() != null) entity.setVinculaTodasCarreras(request.vinculaTodasCarreras());
        entity.setSuperficieCubiertaM2(request.superficieCubiertaM2());
        entity.setSuperficieTotalM2(request.superficieTotalM2());
        entity.setObservaciones(request.observaciones());
        if (request.estadoConeau() != null) entity.setEstadoConeau(request.estadoConeau());
        entity.setActualizadoEn(Instant.now());

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

        // Espacios físicos y subespacios - Sincronizar
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

            // Desactivar los que ya no existen
            for (EspacioFisicoJpaEntity act : actuales) {
                if (!idsEnviados.contains(act.getId())) {
                    act.setActivo(false);
                    espacioFisicoRepository.save(act);
                }
            }

            // Actualizar o crear padres y subespacios
            for (EspacioFisicoRequest espReq : request.espacios()) {
                EspacioFisicoJpaEntity esp;
                if (espReq.id() != null) {
                    esp = espacioFisicoRepository.findById(espReq.id()).orElse(new EspacioFisicoJpaEntity());
                    if (esp.getId() == null) {
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
                esp.setDenominacion(espReq.denominacion());
                esp.setDescripcionInfraestructura(espReq.descripcionInfraestructura() != null ? espReq.descripcionInfraestructura() : espReq.denominacion());
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
                esp.setActivo(true);
                if (espReq.tipoEspacioId() != null) {
                    tipoEspacioRepository.findById(espReq.tipoEspacioId()).ifPresent(esp::setTipoEspacio);
                }
                if (esp.getTipoEspacio() == null) {
                    tipoEspacioRepository.findAll().stream().findFirst().ifPresent(esp::setTipoEspacio);
                }
                espacioFisicoRepository.save(esp);

                // Subespacios hijos
                if (tieneSubs && espReq.subespacios() != null) {
                    for (EspacioFisicoRequest subReq : espReq.subespacios()) {
                        EspacioFisicoJpaEntity sub;
                        if (subReq.id() != null) {
                            sub = espacioFisicoRepository.findById(subReq.id()).orElse(new EspacioFisicoJpaEntity());
                            if (sub.getId() == null) {
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
                        sub.setDenominacion(subReq.denominacion());
                        sub.setDescripcionInfraestructura(subReq.descripcionInfraestructura() != null ? subReq.descripcionInfraestructura() : subReq.denominacion());
                        sub.setCantidad(subReq.cantidad() != null ? subReq.cantidad() : 1);
                        int subCap = subReq.capacidadPersonasSentadas() != null ? subReq.capacidadPersonasSentadas() :
                                (subReq.capacidadEstudiantes() != null ? subReq.capacidadEstudiantes() : 0);
                        sub.setCapacidadPersonasSentadas(subCap);
                        sub.setCapacidadEstudiantes(subCap);
                        sub.setSuperficieM2(subReq.superficieM2());
                        sub.setEquipamientoDisponible(subReq.equipamientoDisponible());
                        sub.setObservacionHigieneSeguridad(subReq.observacionHigieneSeguridad());
                        sub.setEstadoConeau(subReq.estadoConeau() != null ? subReq.estadoConeau() : "CARGADO");
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
                        espacioFisicoRepository.save(sub);
                    }
                }
            }
        }

        // Documentos de respaldo (Adjuntar Documentos en Actualizar Inmueble)
        if (request.documentos() != null && !request.documentos().isEmpty()) {
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'HIGIENE_SEGURIDAD')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<ConsultaEspaciosResponse> consultarEspacios(
            @RequestParam(required = false) UUID tipoEspacioId,
            @RequestParam(required = false) BigDecimal m2Min,
            @RequestParam(required = false) BigDecimal m2Max,
            @RequestParam(required = false) UUID sedeId
    ) {
        List<EspacioFisicoJpaEntity> lista = espacioFisicoRepository.consultarPorFiltros(tipoEspacioId, m2Min, m2Max, sedeId);
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

    // -------------------------------------------------------------------------
    // Gestión por Proceso de Acreditación (Snapshot + Plan de Trabajo)
    // -------------------------------------------------------------------------

    @GetMapping("/procesos/{procesoId}/inmuebles")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional
    public ResponseEntity<?> obtenerInmueblesProceso(@PathVariable UUID procesoId) {
        try {
            List<InmuebleJpaEntity> lista = inmuebleRepository.findByProcesoIdActivos(procesoId);

            // Si aún no tiene snapshot, clonar del padrón maestro
            if (lista.isEmpty()) {
                lista = clonarInmueblesMaestrosParaProceso(procesoId);
            }

            List<InmuebleResponse> response = lista.stream()
                    .map(this::mapToInmuebleResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al procesar snapshot de inmuebles para el proceso " + procesoId, "mensaje", e.getMessage()));
        }
    }

    @PostMapping("/procesos/{procesoId}/inmuebles/copiar-maestro")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
    @Transactional
    public ResponseEntity<VerificarInfraestructuraResponse> verificarYRemitirInfraestructura(
            @PathVariable UUID procesoId
    ) {
        UUID usuarioId = resolverUsuarioAutenticadoId();
        Instant now = Instant.now();

        List<InmuebleJpaEntity> inmuebles = inmuebleRepository.findByProcesoIdActivos(procesoId);
        if (inmuebles.isEmpty()) {
            inmuebles = inmuebleRepository.findMaestrosActivos();
        }

        for (InmuebleJpaEntity inm : inmuebles) {
            inm.setVerificado(true);
            inm.setFechaVerificacion(now);
            inm.setEstadoConeau("VERIFICADO");
            inm.setActualizadoEn(now);
            inmuebleRepository.save(inm);
        }

        String estadoActividad = "SIN_PLAN";
        Optional<PlanTrabajo> planOpt = planTrabajoRepository.findByProcesoId(procesoId);
        if (planOpt.isPresent()) {
            PlanTrabajo plan = planOpt.get();
            Optional<ActividadPlanTrabajo> actOpt = plan.getActividades().stream()
                    .filter(a -> "4.1".equals(a.getCodigoActividad()) || a.getNombre().toLowerCase().contains("infraestructura"))
                    .findFirst();

            if (actOpt.isPresent()) {
                ActividadPlanTrabajo act = actOpt.get();
                act.completar(usuarioId, "Verificación de inmuebles completada por Infraestructura y remitida a AEA");
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
                "Inmuebles verificados exitosamente. Tarea 4.1 finalizada y registrada al 100% en el Plan de Trabajo (hoja INFO)"
        ));
    }

    // -------------------------------------------------------------------------
    // Tipos de Espacio Físico
    // -------------------------------------------------------------------------

    @GetMapping("/tipos-espacio")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA', 'HIGIENE_SEGURIDAD')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<TipoEspacioResponse>> listarTiposEspacio() {
        List<TipoEspacioResponse> response = tipoEspacioRepository.findByActivoTrueOrderByDenominacionAsc().stream()
                .map(t -> new TipoEspacioResponse(t.getId(), t.getCodigo(), t.getDenominacion(), t.getActivo()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tipos-espacio")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'AEA', 'INFRAESTRUCTURA', 'BEDELIA')")
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

    private InmuebleResponse mapToInmuebleResponse(InmuebleJpaEntity entity) {
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
                entity.getVerificado(),
                entity.getFechaVerificacion(),
                entity.getEstadoConeau(),
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
