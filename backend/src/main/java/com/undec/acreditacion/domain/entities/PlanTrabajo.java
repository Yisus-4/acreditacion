package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PlanTrabajo {

    private final UUID id;
    private final UUID procesoId;
    private final String codigo;
    private final TipoPlanTrabajo tipo;
    private EstadoPlanTrabajo estado;
    private final Instant creadoEn;
    private Instant actualizadoEn;
    private final List<ActividadPlanTrabajo> actividades = new ArrayList<>();

    public static PlanTrabajo inicializar(UUID procesoId, String codigo, TipoPlanTrabajo tipo) {
        UUID planId = UUID.randomUUID();
        List<ActividadPlanTrabajo> inicial = new ArrayList<>();
        inicial.add(ActividadPlanTrabajo.crearActividadInicial(planId));
        inicial.add(ActividadPlanTrabajo.crearActividadNormativa(planId));
        Instant now = Instant.now();
        return new PlanTrabajo(
                planId,
                procesoId,
                codigo,
                tipo,
                EstadoPlanTrabajo.ACTIVO,
                now,
                now,
                inicial
        );
    }

    public static PlanTrabajo rehydrate(UUID id,
                                        UUID procesoId,
                                        String codigo,
                                        TipoPlanTrabajo tipo,
                                        EstadoPlanTrabajo estado,
                                        Instant creadoEn,
                                        Instant actualizadoEn,
                                        List<ActividadPlanTrabajo> actividades) {
        return new PlanTrabajo(
                id,
                procesoId,
                codigo,
                tipo,
                estado,
                creadoEn,
                actualizadoEn,
                actividades
        );
    }

    public PlanTrabajo(UUID id,
                       UUID procesoId,
                       String codigo,
                       TipoPlanTrabajo tipo,
                       EstadoPlanTrabajo estado,
                       Instant creadoEn,
                       Instant actualizadoEn,
                       List<ActividadPlanTrabajo> actividades) {
        if (id == null) {
            throw new DomainValidationException("El ID del plan de trabajo es obligatorio");
        }
        if (procesoId == null) {
            throw new DomainValidationException("El ID del proceso es obligatorio");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new DomainValidationException("El código del plan de trabajo es obligatorio");
        }
        if (tipo == null) {
            throw new DomainValidationException("El tipo de plan de trabajo es obligatorio");
        }
        if (estado == null) {
            throw new DomainValidationException("El estado del plan de trabajo es obligatorio");
        }

        this.id = id;
        this.procesoId = procesoId;
        this.codigo = codigo.trim();
        this.tipo = tipo;
        this.estado = estado;
        Instant now = Instant.now();
        this.creadoEn = creadoEn != null ? creadoEn : now;
        this.actualizadoEn = actualizadoEn != null ? actualizadoEn : this.creadoEn;

        if (actividades != null) {
            this.actividades.addAll(actividades);
        }
    }

    public void anexarActividad(int numeroOrden, String codigoActividad, String nombre) {
        if (this.estado != EstadoPlanTrabajo.ACTIVO) {
            throw new DomainValidationException("No se pueden agregar actividades a un plan finalizado");
        }
        if (actividades.stream().anyMatch(a -> a.getNumeroOrden() == numeroOrden)) {
            throw new DomainValidationException("Ya existe una actividad con orden " + numeroOrden);
        }
        ActividadPlanTrabajo nueva = ActividadPlanTrabajo.crear(this.id, numeroOrden, codigoActividad, nombre);
        this.actividades.add(nueva);
        this.actualizadoEn = Instant.now();
    }

    public void establecerCronogramaTentativo(List<ActividadPlanTrabajo> cronograma,
                                            LocalDate fechaLimiteConeau,
                                            UUID usuarioAeaId) {
        if (this.estado != EstadoPlanTrabajo.ACTIVO) {
            throw new DomainValidationException("No se puede establecer el cronograma en un plan finalizado");
        }

        // 1. Asegurar la existencia y completitud de la Actividad 2 (ACT-02-ELABORAR-PLAN-TRABAJO)
        Optional<ActividadPlanTrabajo> act2Opt = this.actividades.stream()
                .filter(a -> ActividadPlanTrabajo.CODIGO_ACT_02.equals(a.getCodigoActividad()))
                .findFirst();

        ActividadPlanTrabajo act2;
        if (act2Opt.isPresent()) {
            act2 = act2Opt.get();
        } else {
            act2 = ActividadPlanTrabajo.crearActividadElaborarPlan(this.id);
            this.actividades.add(act2);
        }

        if (usuarioAeaId != null && act2.getEstado() != EstadoActividad.COMPLETADA) {
            act2.completar(usuarioAeaId);
        }

        // 2. Validar coherencia temporal del cronograma tentativo
        if (cronograma != null) {
            for (ActividadPlanTrabajo act : cronograma) {
                if (fechaLimiteConeau != null) {
                    if (act.getFechaFinEstimada() != null && act.getFechaFinEstimada().isAfter(fechaLimiteConeau)) {
                        throw new DomainValidationException(
                                "La fecha fin estimada de la actividad '" + act.getNombre() + "' (" +
                                act.getFechaFinEstimada() + ") no puede ser posterior a la fecha límite CONEAU (" +
                                fechaLimiteConeau + ")"
                        );
                    }
                    if (act.getFechaInicioEstimada() != null && act.getFechaInicioEstimada().isAfter(fechaLimiteConeau)) {
                        throw new DomainValidationException(
                                "La fecha de inicio estimada de la actividad '" + act.getNombre() + "' (" +
                                act.getFechaInicioEstimada() + ") no puede ser posterior a la fecha límite CONEAU (" +
                                fechaLimiteConeau + ")"
                        );
                    }
                }
            }

            // 3. Buscar si existían actividades troncales (1.3, ACT-04, ACT-06) previas en el plan para preservar su ID y estado
            Optional<ActividadPlanTrabajo> normativaPreviaOpt = this.actividades.stream()
                    .filter(PlanTrabajo::esActividadNormativa)
                    .findFirst();
            Optional<ActividadPlanTrabajo> act4PreviaOpt = this.actividades.stream()
                    .filter(PlanTrabajo::esActividadIdentificarDocumentacionInteres)
                    .findFirst();
            Optional<ActividadPlanTrabajo> act6PreviaOpt = this.actividades.stream()
                    .filter(PlanTrabajo::esActividadFormalizacionElectronica)
                    .findFirst();

            // Reemplazar actividades de cronograma previas (orden > 2)
            this.actividades.removeIf(a -> a.getNumeroOrden() > 2);

            // 4. Incorporar nuevas actividades asegurando orden único secuencial
            boolean normativaIncorporada = false;
            boolean act4Incorporada = false;
            boolean act6Incorporada = false;
            int ordenActual = 3;
            for (ActividadPlanTrabajo act : cronograma) {
                boolean esNormativa = esActividadNormativa(act);
                boolean esAct4 = esActividadIdentificarDocumentacionInteres(act);
                boolean esAct6 = esActividadFormalizacionElectronica(act);

                int orden = act.getNumeroOrden() >= 3 ? act.getNumeroOrden() : ordenActual;
                // Si ya existe alguna con ese orden, usamos el correlativo secuencial
                final int finalOrden = orden;
                if (this.actividades.stream().anyMatch(a -> a.getNumeroOrden() == finalOrden)) {
                    orden = ordenActual;
                }

                ActividadPlanTrabajo nueva;
                if (esNormativa && normativaPreviaOpt.isPresent()) {
                    normativaIncorporada = true;
                    nueva = fusionarConPrevia(normativaPreviaOpt.get(), act, orden);
                } else if (esAct4 && act4PreviaOpt.isPresent()) {
                    act4Incorporada = true;
                    nueva = fusionarConPrevia(act4PreviaOpt.get(), act, orden);
                } else if (esAct6 && act6PreviaOpt.isPresent()) {
                    act6Incorporada = true;
                    nueva = fusionarConPrevia(act6PreviaOpt.get(), act, orden);
                } else {
                    nueva = ActividadPlanTrabajo.rehydrate(
                            act.getId(),
                            this.id,
                            orden,
                            act.getCodigoActividad(),
                            act.getNombre(),
                            act.getEstado(),
                            act.getFechaReunionCoordinacion(),
                            act.getSintesisAcuerdos(),
                            act.getCompletadaEn(),
                            act.getCompletadaPor(),
                            act.getHoja(),
                            act.getFechaInicioEstimada(),
                            act.getFechaFinEstimada(),
                            act.getResponsable(),
                            act.getPrioridad(),
                            act.getCreadoEn(),
                            act.getActualizadoEn()
                    );
                    nueva.restaurarRegistroComunicacion(act.getAreasInformadas(), act.getDocumentacionRemitida());
                }
                this.actividades.add(nueva);
                ordenActual = Math.max(ordenActual, orden) + 1;
            }

            // Preservar actividades troncales previas que no hayan venido en el cronograma enviado
            if (normativaPreviaOpt.isPresent() && !normativaIncorporada) {
                ordenActual = preservarActividadPrevia(normativaPreviaOpt.get(), ordenActual);
            }
            if (act4PreviaOpt.isPresent() && !act4Incorporada) {
                ordenActual = preservarActividadPrevia(act4PreviaOpt.get(), ordenActual);
            }
            if (act6PreviaOpt.isPresent() && !act6Incorporada) {
                preservarActividadPrevia(act6PreviaOpt.get(), ordenActual);
            }

            // Re-normalizar numeroOrden de forma estrictamente secuencial y única para evitar colisiones
            List<ActividadPlanTrabajo> normalizadas = new ArrayList<>();
            int seq = 1;
            for (ActividadPlanTrabajo a : this.actividades) {
                if (a.getNumeroOrden() == seq) {
                    normalizadas.add(a);
                } else {
                    ActividadPlanTrabajo reordenada = ActividadPlanTrabajo.rehydrate(
                            a.getId(),
                            a.getPlanTrabajoId(),
                            seq,
                            a.getCodigoActividad(),
                            a.getNombre(),
                            a.getEstado(),
                            a.getFechaReunionCoordinacion(),
                            a.getSintesisAcuerdos(),
                            a.getCompletadaEn(),
                            a.getCompletadaPor(),
                            a.getHoja(),
                            a.getFechaInicioEstimada(),
                            a.getFechaFinEstimada(),
                            a.getResponsable(),
                            a.getPrioridad(),
                            a.getCreadoEn(),
                            Instant.now()
                    );
                    reordenada.restaurarRegistroComunicacion(a.getAreasInformadas(), a.getDocumentacionRemitida());
                    normalizadas.add(reordenada);
                }
                seq++;
            }
            this.actividades.clear();
            this.actividades.addAll(normalizadas);
        }

        this.actualizadoEn = Instant.now();
    }

    private ActividadPlanTrabajo fusionarConPrevia(ActividadPlanTrabajo prev, ActividadPlanTrabajo act, int orden) {
        ActividadPlanTrabajo nueva = ActividadPlanTrabajo.rehydrate(
                prev.getId(),
                this.id,
                orden,
                prev.getCodigoActividad(),
                act.getNombre() != null ? act.getNombre() : prev.getNombre(),
                prev.getEstado(),
                prev.getFechaReunionCoordinacion(),
                prev.getSintesisAcuerdos(),
                prev.getCompletadaEn(),
                prev.getCompletadaPor(),
                act.getHoja() != null ? act.getHoja() : prev.getHoja(),
                act.getFechaInicioEstimada() != null ? act.getFechaInicioEstimada() : prev.getFechaInicioEstimada(),
                act.getFechaFinEstimada() != null ? act.getFechaFinEstimada() : prev.getFechaFinEstimada(),
                act.getResponsable() != null ? act.getResponsable() : prev.getResponsable(),
                act.getPrioridad() != null ? act.getPrioridad() : prev.getPrioridad(),
                prev.getCreadoEn(),
                Instant.now()
        );
        nueva.restaurarRegistroComunicacion(prev.getAreasInformadas(), prev.getDocumentacionRemitida());
        return nueva;
    }

    private int preservarActividadPrevia(ActividadPlanTrabajo prev, int ordenActual) {
        int orden = prev.getNumeroOrden();
        final int ordFinal = orden;
        if (this.actividades.stream().anyMatch(a -> a.getNumeroOrden() == ordFinal)) {
            orden = ordenActual;
        }
        while (existeOrden(orden)) {
            orden++;
        }
        ActividadPlanTrabajo preservada = ActividadPlanTrabajo.rehydrate(
                prev.getId(),
                this.id,
                orden,
                prev.getCodigoActividad(),
                prev.getNombre(),
                prev.getEstado(),
                prev.getFechaReunionCoordinacion(),
                prev.getSintesisAcuerdos(),
                prev.getCompletadaEn(),
                prev.getCompletadaPor(),
                prev.getHoja(),
                prev.getFechaInicioEstimada(),
                prev.getFechaFinEstimada(),
                prev.getResponsable(),
                prev.getPrioridad(),
                prev.getCreadoEn(),
                Instant.now()
        );
        preservada.restaurarRegistroComunicacion(prev.getAreasInformadas(), prev.getDocumentacionRemitida());
        this.actividades.add(preservada);
        return Math.max(ordenActual, orden) + 1;
    }

    public void agregarActividad(ActividadPlanTrabajo actividad) {
        if (actividad == null) {
            return;
        }
        final UUID targetId = actividad.getId();
        final String targetCodigo = actividad.getCodigoActividad();
        boolean yaExiste = this.actividades.stream()
                .anyMatch(a -> a.getId().equals(targetId) ||
                        (targetCodigo != null && targetCodigo.equals(a.getCodigoActividad())));
        if (!yaExiste) {
            int orden = actividad.getNumeroOrden();
            while (existeOrden(orden)) {
                orden++;
            }
            ActividadPlanTrabajo paraAgregar = actividad;
            if (orden != actividad.getNumeroOrden()) {
                paraAgregar = ActividadPlanTrabajo.rehydrate(
                        actividad.getId(),
                        this.id,
                        orden,
                        actividad.getCodigoActividad(),
                        actividad.getNombre(),
                        actividad.getEstado(),
                        actividad.getFechaReunionCoordinacion(),
                        actividad.getSintesisAcuerdos(),
                        actividad.getCompletadaEn(),
                        actividad.getCompletadaPor(),
                        actividad.getHoja(),
                        actividad.getFechaInicioEstimada(),
                        actividad.getFechaFinEstimada(),
                        actividad.getResponsable(),
                        actividad.getPrioridad(),
                        actividad.getCreadoEn(),
                        actividad.getActualizadoEn()
                );
                paraAgregar.restaurarRegistroComunicacion(actividad.getAreasInformadas(), actividad.getDocumentacionRemitida());
            }
            this.actividades.add(paraAgregar);
            this.actualizadoEn = Instant.now();
        }
    }

    public boolean eliminarActividadSiExiste(String codigoActividad) {
        if (codigoActividad == null) return false;
        boolean removed = this.actividades.removeIf(a ->
                codigoActividad.equals(a.getCodigoActividad())
                        || (a.getNombre() != null && a.getNombre().toLowerCase().contains(codigoActividad.toLowerCase()))
        );
        if (removed) {
            this.actualizadoEn = Instant.now();
        }
        return removed;
    }

    private boolean existeOrden(int orden) {
        final int targetOrden = orden;
        return this.actividades.stream().anyMatch(a -> a.getNumeroOrden() == targetOrden);
    }

    public static boolean esActividadNormativa(ActividadPlanTrabajo a) {
        if (a == null) return false;
        if ("1.3".equals(a.getCodigoActividad()) || ActividadPlanTrabajo.CODIGO_ACT_03.equals(a.getCodigoActividad())) {
            return true;
        }
        return a.getNombre() != null && a.getNombre().toLowerCase().contains("normativa");
    }

    public static boolean esActividadIdentificarDocumentacionInteres(ActividadPlanTrabajo a) {
        if (a == null) return false;
        if (ActividadPlanTrabajo.CODIGO_ACT_04.equals(a.getCodigoActividad()) || "1.5".equals(a.getCodigoActividad())) {
            return true;
        }
        return a.getNombre() != null && a.getNombre().toLowerCase().contains("documentación de interés");
    }

    public static boolean esActividadFormalizacionElectronica(ActividadPlanTrabajo a) {
        if (a == null) return false;
        if (ActividadPlanTrabajo.CODIGO_ACT_06.equals(a.getCodigoActividad()) || "1.8".equals(a.getCodigoActividad())) {
            return true;
        }
        return a.getNombre() != null && a.getNombre().toLowerCase().contains("formalización electrónica");
    }

    public List<ActividadPlanTrabajo> getActividadesInfo() {
        return this.actividades.stream()
                .filter(a -> a.getHoja() == HojaPlanTrabajo.INFO)
                .collect(Collectors.toUnmodifiableList());
    }

    public List<ActividadPlanTrabajo> getActividadesConeau() {
        return this.actividades.stream()
                .filter(a -> a.getHoja() == HojaPlanTrabajo.CONEAU)
                .collect(Collectors.toUnmodifiableList());
    }

    public void finalizar() {
        this.estado = EstadoPlanTrabajo.FINALIZADO;
        this.actualizadoEn = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public String getCodigo() {
        return codigo;
    }

    public TipoPlanTrabajo getTipo() {
        return tipo;
    }

    public EstadoPlanTrabajo getEstado() {
        return estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<ActividadPlanTrabajo> getActividades() {
        return Collections.unmodifiableList(actividades);
    }

    public double getPorcentajeProgreso() {
        if (actividades.isEmpty()) {
            return 0.0;
        }
        long completadas = actividades.stream()
                .filter(a -> a.getEstado() == EstadoActividad.COMPLETADA)
                .count();
        return (double) completadas / actividades.size() * 100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlanTrabajo that = (PlanTrabajo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
