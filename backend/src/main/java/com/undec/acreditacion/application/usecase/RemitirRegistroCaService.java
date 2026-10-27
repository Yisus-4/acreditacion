package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.MiembroCaInputDto;
import com.undec.acreditacion.application.input.RemitirRegistroCaUseCase;
import com.undec.acreditacion.application.output.RegistroCaDto;
import com.undec.acreditacion.domain.entities.DocumentoAcreditacion;
import com.undec.acreditacion.domain.entities.MiembroComisionAutoevaluacion;
import com.undec.acreditacion.domain.entities.RegistroConformacionCa;
import com.undec.acreditacion.domain.exceptions.DomainValidationException;
import com.undec.acreditacion.domain.exceptions.ResourceNotFoundException;
import com.undec.acreditacion.domain.ports.DocumentStoragePort;
import com.undec.acreditacion.domain.ports.ProcesoAcreditacionRepositoryPort;
import com.undec.acreditacion.domain.ports.RegistroConformacionCaRepositoryPort;

import java.util.List;
import java.util.UUID;

public class RemitirRegistroCaService implements RemitirRegistroCaUseCase {

    private static final String CARPETA_CONVOCATORIA = "01-Convocatoria y formalizacion CONEAU/";

    private final RegistroConformacionCaRepositoryPort registroCaRepository;
    private final ProcesoAcreditacionRepositoryPort procesoRepository;
    private final DocumentStoragePort storagePort;

    public RemitirRegistroCaService(
            RegistroConformacionCaRepositoryPort registroCaRepository,
            ProcesoAcreditacionRepositoryPort procesoRepository,
            DocumentStoragePort storagePort
    ) {
        this.registroCaRepository = registroCaRepository;
        this.procesoRepository = procesoRepository;
        this.storagePort = storagePort;
    }

    @Override
    public RegistroCaDto remitir(
            UUID procesoId,
            UUID usuarioDcId,
            boolean coincideCurricular,
            List<MiembroCaInputDto> miembros,
            byte[] resolucionBytes,
            String resolucionNombreOriginal
    ) {
        procesoRepository.findById(procesoId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró el proceso de acreditación con ID: " + procesoId));

        RegistroConformacionCa ca = registroCaRepository.findByProcesoId(procesoId)
                .orElseGet(() -> new RegistroConformacionCa(null, procesoId));

        String storagePath = null;
        String nombreFinal = null;
        Long tamanio = null;

        if (resolucionBytes != null && resolucionBytes.length > 0) {
            nombreFinal = (resolucionNombreOriginal != null && !resolucionNombreOriginal.isBlank())
                    ? resolucionNombreOriginal.trim()
                    : "Resolucion_Designacion_CA.pdf";
            String objectPath = CARPETA_CONVOCATORIA + "CA/Resoluciones/" + UUID.randomUUID() + "_" + nombreFinal;
            storagePath = storagePort.uploadFile(resolucionBytes, objectPath, DocumentoAcreditacion.MIME_PDF);
            tamanio = (long) resolucionBytes.length;
        }

        List<MiembroComisionAutoevaluacion> listaMiembros = RegistroCaMapper.toDomainList(miembros);
        ca.remitir(usuarioDcId, coincideCurricular, listaMiembros, storagePath, nombreFinal, tamanio);

        RegistroConformacionCa guardado = registroCaRepository.save(ca);
        return RegistroCaMapper.toDto(guardado);
    }
}
