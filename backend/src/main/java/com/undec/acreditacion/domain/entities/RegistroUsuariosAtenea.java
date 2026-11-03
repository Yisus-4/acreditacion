package com.undec.acreditacion.domain.entities;

import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.services.CatalogoMatrizPermisosAtenea;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RegistroUsuariosAtenea {

    private UUID id;
    private UUID procesoId;
    private EstadoRegistroUsuariosAtenea estado;
    private LocalDate fechaLimite;
    private Instant generadoEn;
    private Instant aprobadoEn;
    private UUID creadoPor;
    private String rutaDocumento;
    private Instant creadoEn;
    private Instant actualizadoEn;
    private final List<UsuarioPermisoAtenea> usuarios = new ArrayList<>();
    private final List<ItemMatrizPermisoAtenea> matrizPermisos = new ArrayList<>();

    public RegistroUsuariosAtenea() {
        this.id = UUID.randomUUID();
        this.estado = EstadoRegistroUsuariosAtenea.EN_DEFINICION;
        this.creadoEn = Instant.now();
        this.actualizadoEn = Instant.now();
    }

    public static RegistroUsuariosAtenea iniciar(UUID procesoId, UUID creadoPor, LocalDate fechaInicio) {
        if (procesoId == null) {
            throw new DomainValidationException("El proceso de acreditación es obligatorio");
        }
        LocalDate inicio = fechaInicio != null ? fechaInicio : LocalDate.now();
        RegistroUsuariosAtenea r = new RegistroUsuariosAtenea();
        r.id = UUID.randomUUID();
        r.procesoId = procesoId;
        r.estado = EstadoRegistroUsuariosAtenea.EN_DEFINICION;
        r.creadoPor = creadoPor;
        // AEA dispone de un plazo no mayor a dos (2) días hábiles
        r.fechaLimite = CalculadorDiasHabiles.calcularFechaLimite(inicio, 2);
        r.creadoEn = Instant.now();
        r.actualizadoEn = Instant.now();
        r.matrizPermisos.addAll(CatalogoMatrizPermisosAtenea.crearMatrizPorDefecto(r.id));
        return r;
    }

    public static RegistroUsuariosAtenea rehydrate(
            UUID id,
            UUID procesoId,
            EstadoRegistroUsuariosAtenea estado,
            LocalDate fechaLimite,
            Instant generadoEn,
            Instant aprobadoEn,
            UUID creadoPor,
            String rutaDocumento,
            Instant creadoEn,
            Instant actualizadoEn,
            List<UsuarioPermisoAtenea> usuarios
    ) {
        return rehydrate(
                id, procesoId, estado, fechaLimite, generadoEn, aprobadoEn,
                creadoPor, rutaDocumento, creadoEn, actualizadoEn, usuarios, null
        );
    }

    public static RegistroUsuariosAtenea rehydrate(
            UUID id,
            UUID procesoId,
            EstadoRegistroUsuariosAtenea estado,
            LocalDate fechaLimite,
            Instant generadoEn,
            Instant aprobadoEn,
            UUID creadoPor,
            String rutaDocumento,
            Instant creadoEn,
            Instant actualizadoEn,
            List<UsuarioPermisoAtenea> usuarios,
            List<ItemMatrizPermisoAtenea> matrizPermisos
    ) {
        RegistroUsuariosAtenea r = new RegistroUsuariosAtenea();
        r.id = id;
        r.procesoId = procesoId;
        r.estado = estado;
        r.fechaLimite = fechaLimite;
        r.generadoEn = generadoEn;
        r.aprobadoEn = aprobadoEn;
        r.creadoPor = creadoPor;
        r.rutaDocumento = rutaDocumento;
        r.creadoEn = creadoEn;
        r.actualizadoEn = actualizadoEn;
        if (usuarios != null) {
            for (UsuarioPermisoAtenea u : usuarios) {
                u.setRegistroId(id);
                r.usuarios.add(u);
            }
        }
        if (matrizPermisos != null && !matrizPermisos.isEmpty()) {
            for (ItemMatrizPermisoAtenea m : matrizPermisos) {
                m.setRegistroId(id);
                r.matrizPermisos.add(m);
            }
        } else {
            r.matrizPermisos.addAll(CatalogoMatrizPermisosAtenea.crearMatrizPorDefecto(id));
        }
        return r;
    }

    public void importarMiembrosCa(List<MiembroComisionAutoevaluacion> miembrosCa) {
        if (miembrosCa == null || miembrosCa.isEmpty()) {
            return;
        }
        for (MiembroComisionAutoevaluacion m : miembrosCa) {
            String nombre = m.getPrimerNombre();
            if (m.getSegundoNombre() != null && !m.getSegundoNombre().isBlank()) {
                nombre += " " + m.getSegundoNombre().trim();
            }
            String cargo = m.getCargoFuncion();
            if (cargo == null || cargo.isBlank()) {
                cargo = m.getClaustroRol() != null ? m.getClaustroRol().name() : "Miembro CA";
            }

            UsuarioPermisoAtenea u = UsuarioPermisoAtenea.crear(
                    GrupoUsuarioAtenea.COMISION_AUTOEVALUACION,
                    nombre,
                    m.getApellido(),
                    m.getDniCuil(),
                    m.getCorreo(),
                    "Comisión de Autoevaluación",
                    cargo,
                    TipoAccesoAtenea.CARGA_Y_VISUALIZACION,
                    "Formulario CONEAU Global ATENEA - Carrera",
                    true,
                    false,
                    false
            );
            agregarUsuario(u);
        }
        this.actualizadoEn = Instant.now();
    }

    public void agregarUsuario(UsuarioPermisoAtenea usuario) {
        if (usuario == null) {
            throw new DomainValidationException("El usuario a agregar no puede ser nulo");
        }
        usuario.setRegistroId(this.id);
        this.usuarios.add(usuario);
        this.actualizadoEn = Instant.now();
    }

    public void actualizarUsuarios(List<UsuarioPermisoAtenea> nuevosUsuarios) {
        if (this.estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            throw new DomainValidationException("No se pueden modificar usuarios de un registro ya finalizado");
        }
        this.usuarios.clear();
        if (nuevosUsuarios != null) {
            for (UsuarioPermisoAtenea u : nuevosUsuarios) {
                u.setRegistroId(this.id);
                this.usuarios.add(u);
            }
        }
        this.actualizadoEn = Instant.now();
    }

    public void actualizarMatrizPermisos(List<ItemMatrizPermisoAtenea> nuevaMatriz) {
        if (this.estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            throw new DomainValidationException("No se puede modificar la matriz de permisos de un registro ya finalizado");
        }
        this.matrizPermisos.clear();
        if (nuevaMatriz != null && !nuevaMatriz.isEmpty()) {
            for (ItemMatrizPermisoAtenea item : nuevaMatriz) {
                item.setRegistroId(this.id);
                this.matrizPermisos.add(item);
            }
        } else {
            this.matrizPermisos.addAll(CatalogoMatrizPermisosAtenea.crearMatrizPorDefecto(this.id));
        }
        this.actualizadoEn = Instant.now();
    }

    public void inicializarMatrizPorDefectoSiVacia() {
        if (this.matrizPermisos.isEmpty()) {
            this.matrizPermisos.addAll(CatalogoMatrizPermisosAtenea.crearMatrizPorDefecto(this.id));
            this.actualizadoEn = Instant.now();
        }
    }

    public void actualizarPlazoDiasHabiles(int diasHabiles) {
        if (this.estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            throw new DomainValidationException("No se puede modificar el plazo de un registro de usuarios ATENEA ya finalizado");
        }
        int dias = diasHabiles > 0 ? diasHabiles : 2;
        this.fechaLimite = CalculadorDiasHabiles.calcularFechaLimite(LocalDate.now(), dias);
        this.actualizadoEn = Instant.now();
    }

    public void extenderPlazoFecha(LocalDate nuevaFechaLimite) {
        if (this.estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            throw new DomainValidationException("No se puede modificar el plazo de un registro de usuarios ATENEA ya finalizado");
        }
        if (nuevaFechaLimite == null) {
            throw new DomainValidationException("Debe seleccionar una fecha válida para el nuevo plazo");
        }
        if (this.fechaLimite != null && !nuevaFechaLimite.isAfter(this.fechaLimite)) {
            throw new DomainValidationException(
                    "El nuevo plazo (" + nuevaFechaLimite + ") debe ser posterior al plazo actual (" +
                    this.fechaLimite + "). No se puede volver a setear un tiempo hacia atrás."
            );
        }
        if (nuevaFechaLimite.isBefore(LocalDate.now())) {
            throw new DomainValidationException(
                    "El plazo no puede establecerse en una fecha anterior a hoy (" + LocalDate.now() + ")."
            );
        }
        this.fechaLimite = nuevaFechaLimite;
        this.actualizadoEn = Instant.now();
    }

    public void finalizar(UUID usuarioAeaId, String rutaDocumento) {
        if (this.estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            throw new DomainValidationException("El registro de usuarios de ATENEA ya se encuentra finalizado");
        }
        if (this.usuarios.isEmpty()) {
            throw new DomainValidationException("Debe existir al menos un usuario registrado para finalizar el RG-003");
        }
        if (rutaDocumento == null || rutaDocumento.isBlank()) {
            throw new DomainValidationException("La ruta del documento generado es obligatoria");
        }

        this.estado = EstadoRegistroUsuariosAtenea.FINALIZADO;
        Instant now = Instant.now();
        this.generadoEn = now;
        this.aprobadoEn = now;
        this.rutaDocumento = rutaDocumento.trim();
        this.actualizadoEn = now;
    }

    public int getDiasRestantes() {
        if (fechaLimite == null) return 0;
        return CalculadorDiasHabiles.calcularDiasHabilesRestantes(LocalDate.now(), fechaLimite);
    }

    public String getSemaforoTexto() {
        if (estado == EstadoRegistroUsuariosAtenea.FINALIZADO) {
            return "Completado";
        }
        if (fechaLimite == null) {
            return "Sin plazo definido";
        }
        int dias = getDiasRestantes();
        if (dias > 1) {
            return "Dentro del plazo (" + dias + " días hábiles restantes)";
        } else if (dias == 1) {
            return "Atención: vence mañana (1 día hábil restante)";
        } else if (dias == 0) {
            return "Atención: vence hoy";
        } else {
            return "Plazo vencido (" + Math.abs(dias) + " días hábiles superados)";
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getProcesoId() {
        return procesoId;
    }

    public EstadoRegistroUsuariosAtenea getEstado() {
        return estado;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDate fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public Instant getGeneradoEn() {
        return generadoEn;
    }

    public Instant getAprobadoEn() {
        return aprobadoEn;
    }

    public UUID getCreadoPor() {
        return creadoPor;
    }

    public String getRutaDocumento() {
        return rutaDocumento;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public List<UsuarioPermisoAtenea> getUsuarios() {
        return Collections.unmodifiableList(usuarios);
    }

    public List<ItemMatrizPermisoAtenea> getMatrizPermisos() {
        return Collections.unmodifiableList(matrizPermisos);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegistroUsuariosAtenea that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
