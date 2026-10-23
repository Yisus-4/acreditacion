package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ActualizarInstitucionUseCase;
import com.undec.acreditacion.application.input.RegistrarInstitucionCommand;
import com.undec.acreditacion.domain.entities.AutoridadInstitucional;
import com.undec.acreditacion.domain.entities.Institucion;
import com.undec.acreditacion.domain.entities.SedeCentral;
import com.undec.acreditacion.domain.entities.TipoAutoridad;
import com.undec.acreditacion.domain.exceptions.ArchivoInvalidoException;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.InstitucionDuplicadaException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.InstitucionRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public class ActualizarInstitucionService implements ActualizarInstitucionUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActualizarInstitucionService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/jpg");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg");

    private final InstitucionRepositoryPort institucionRepository;
    private final DocumentStoragePort documentStoragePort;

    public ActualizarInstitucionService(InstitucionRepositoryPort institucionRepository,
                                      DocumentStoragePort documentStoragePort) {
        this.institucionRepository = Objects.requireNonNull(institucionRepository, "institucionRepository must not be null");
        this.documentStoragePort = Objects.requireNonNull(documentStoragePort, "documentStoragePort must not be null");
    }

    @Override
    public Institucion actualizar(UUID id, RegistrarInstitucionCommand command) {
        if (id == null) {
            throw new ResourceNotFoundException("Institución no encontrada: null");
        }
        if (command == null) {
            throw new DomainValidationException("El comando de actualización no puede ser nulo");
        }

        Institucion institucion = institucionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Institución no encontrada: " + id));

        validarCamposObligatorios(command);
        validarCorreos(command);

        if (!institucion.getNombre().trim().equalsIgnoreCase(command.nombre().trim())) {
            institucionRepository.findByNombre(command.nombre().trim()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new InstitucionDuplicadaException("La institución ingresada ya se encuentra registrada en el sistema");
                }
            });
        }

        if (!institucion.getSigla().trim().equalsIgnoreCase(command.sigla().trim())) {
            institucionRepository.findBySigla(command.sigla().trim()).ifPresent(existente -> {
                if (!existente.getId().equals(id)) {
                    throw new InstitucionDuplicadaException("La institución ingresada ya se encuentra registrada en el sistema");
                }
            });
        }

        String nuevoLogoPath = null;
        if (command.logoBytes() != null && command.logoBytes().length > 0) {
            validarLogotipo(command);
            nuevoLogoPath = documentStoragePort.uploadFile(
                    command.logoBytes(),
                    command.logoFilename() != null ? command.logoFilename() : "logo.png",
                    command.logoContentType() != null ? command.logoContentType() : "image/png"
            );
        }

        UUID maxId = institucion.getMaximaAutoridad().map(AutoridadInstitucional::getId).orElseGet(UUID::randomUUID);
        Instant maxCreadoEn = institucion.getMaximaAutoridad().map(AutoridadInstitucional::getCreadoEn).orElseGet(Instant::now);
        AutoridadInstitucional maximaAutoridad = AutoridadInstitucional.rehydrate(
                maxId,
                TipoAutoridad.MAXIMA_AUTORIDAD,
                command.maximaAutoridadApellido(),
                command.maximaAutoridadPrimerNombre(),
                command.maximaAutoridadSegundoNombre(),
                command.maximaAutoridadTelefono(),
                command.maximaAutoridadEmail(),
                null,
                null,
                maxCreadoEn
        );

        UUID adminId = institucion.getAdministradorInstitucional().map(AutoridadInstitucional::getId).orElseGet(UUID::randomUUID);
        Instant adminCreadoEn = institucion.getAdministradorInstitucional().map(AutoridadInstitucional::getCreadoEn).orElseGet(Instant::now);
        AutoridadInstitucional administradorInstitucional = AutoridadInstitucional.rehydrate(
                adminId,
                TipoAutoridad.ADMINISTRADOR_INSTITUCIONAL,
                command.adminInstitucionalApellido(),
                command.adminInstitucionalPrimerNombre(),
                command.adminInstitucionalSegundoNombre(),
                command.adminInstitucionalTelefono(),
                command.adminInstitucionalEmail(),
                command.adminInstitucionalCargo(),
                command.adminInstitucionalAmbito(),
                adminCreadoEn
        );

        UUID sedeId = institucion.getSedeCentral() != null ? institucion.getSedeCentral().getId() : UUID.randomUUID();
        Instant sedeCreadoEn = institucion.getSedeCentral() != null ? institucion.getSedeCentral().getCreadoEn() : Instant.now();
        SedeCentral sedeCentral = SedeCentral.rehydrate(
                sedeId,
                true,
                command.calle(),
                command.numero(),
                command.piso(),
                command.departamento(),
                command.codigoPostal(),
                command.provinciaId(),
                command.departamentoId(),
                command.localidadId(),
                sedeCreadoEn
        );

        Institucion actualizada = institucion.actualizar(
                command.nombre(),
                command.sigla(),
                nuevoLogoPath,
                maximaAutoridad,
                administradorInstitucional,
                sedeCentral
        );

        try {
            return institucionRepository.save(actualizada);
        } catch (Exception e) {
            if (nuevoLogoPath != null) {
                try {
                    documentStoragePort.deleteFile(nuevoLogoPath);
                } catch (Exception ex) {
                    log.warn("Error al intentar compensar el archivo subido '{}' tras fallo al actualizar institución: {}", nuevoLogoPath, ex.getMessage());
                }
            }
            throw e;
        }
    }

    private void validarCamposObligatorios(RegistrarInstitucionCommand command) {
        requireText(command.nombre(), "El nombre de la institución es obligatorio");
        requireText(command.sigla(), "La sigla de la institución es obligatoria");

        requireText(command.maximaAutoridadApellido(), "El apellido de la máxima autoridad es obligatorio");
        requireText(command.maximaAutoridadPrimerNombre(), "El primer nombre de la máxima autoridad es obligatorio");
        requireText(command.maximaAutoridadTelefono(), "El teléfono de la máxima autoridad es obligatorio");
        requireText(command.maximaAutoridadEmail(), "El correo electrónico de la máxima autoridad es obligatorio");

        requireText(command.adminInstitucionalApellido(), "El apellido del administrador institucional es obligatorio");
        requireText(command.adminInstitucionalPrimerNombre(), "El primer nombre del administrador institucional es obligatorio");
        requireText(command.adminInstitucionalTelefono(), "El teléfono del administrador institucional es obligatorio");
        requireText(command.adminInstitucionalEmail(), "El correo electrónico del administrador institucional es obligatorio");
        requireText(command.adminInstitucionalCargo(), "El cargo del administrador institucional es obligatorio");
        requireText(command.adminInstitucionalAmbito(), "El ámbito del administrador institucional es obligatorio");

        requireText(command.calle(), "La calle de la sede central es obligatoria");
        requireText(command.numero(), "El número de la sede central es obligatorio");
        requireText(command.codigoPostal(), "El código postal es obligatorio");
        if (command.provinciaId() == null) {
            throw new DomainValidationException("La provincia de la sede central es obligatoria");
        }
        if (command.departamentoId() == null) {
            throw new DomainValidationException("El departamento de la sede central es obligatorio");
        }
        if (command.localidadId() == null) {
            throw new DomainValidationException("La localidad de la sede central es obligatoria");
        }
    }

    private void validarLogotipo(RegistrarInstitucionCommand command) {
        String contentType = command.logoContentType() != null ? command.logoContentType().toLowerCase().trim() : "";
        String filename = command.logoFilename() != null ? command.logoFilename().toLowerCase().trim() : "";

        boolean matchesContentType = ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean matchesExtension = ALLOWED_EXTENSIONS.stream().anyMatch(filename::endsWith);

        if (!matchesContentType && !matchesExtension) {
            throw new ArchivoInvalidoException("Debe adjuntar un formato de imagen válido (PNG o JPG)");
        }
    }

    private void validarCorreos(RegistrarInstitucionCommand command) {
        if (!EMAIL_PATTERN.matcher(command.maximaAutoridadEmail().trim()).matches()) {
            throw new DomainValidationException("El correo electrónico de la máxima autoridad tiene un formato inválido");
        }
        if (!EMAIL_PATTERN.matcher(command.adminInstitucionalEmail().trim()).matches()) {
            throw new DomainValidationException("El correo electrónico del administrador institucional tiene un formato inválido");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new DomainValidationException(message);
        }
    }
}
