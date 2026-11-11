package com.undec.acreditacion.infrastructure.autoevaluacion.service;

import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.GuiaHigieneDocumentoJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.GuiaHigieneSeguridadJpaEntity;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataGuiaHigieneDocumentoRepository;
import com.undec.acreditacion.infrastructure.autoevaluacion.persistence.SpringDataGuiaHigieneSeguridadRepository;
import com.undec.acreditacion.infrastructure.persistence.postgres.InstitucionJpaEntity;
import com.undec.acreditacion.infrastructure.persistence.postgres.SpringDataInstitucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class GuiaHigieneSeguridadClonerService {

    private final SpringDataGuiaHigieneSeguridadRepository guiaRepository;
    private final SpringDataGuiaHigieneDocumentoRepository documentoRepository;
    private final SpringDataInstitucionRepository institucionRepository;

    public GuiaHigieneSeguridadClonerService(
            SpringDataGuiaHigieneSeguridadRepository guiaRepository,
            SpringDataGuiaHigieneDocumentoRepository documentoRepository,
            SpringDataInstitucionRepository institucionRepository
    ) {
        this.guiaRepository = guiaRepository;
        this.documentoRepository = documentoRepository;
        this.institucionRepository = institucionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GuiaHigieneSeguridadJpaEntity clonarGuiaMaestraParaProceso(UUID procesoId) {
        GuiaHigieneSeguridadJpaEntity maestro = guiaRepository.findGuiaMaestraCompleta()
                .orElseThrow(() -> new IllegalStateException("No existe Guía Maestra GT-003 en el sistema"));

        InstitucionJpaEntity institucion = institucionRepository.findAll().stream().findFirst()
                .orElse(maestro.getInstitucion());

        GuiaHigieneSeguridadJpaEntity c = new GuiaHigieneSeguridadJpaEntity();
        c.setId(UUID.randomUUID());
        c.setProcesoId(procesoId);
        c.setInstitucion(institucion);
        c.setEsMaestro(false);
        c.setEdicion(maestro.getEdicion());
        c.setFechaVigencia(maestro.getFechaVigencia());
        c.setElaboradoPor(maestro.getElaboradoPor());
        c.setRevisadoPor(maestro.getRevisadoPor());
        c.setInstanciaResponsableUa(maestro.getInstanciaResponsableUa());
        c.setPoliticaUnidadesApoyo(maestro.getPoliticaUnidadesApoyo());
        c.setAplicaUnidadesApoyo(maestro.getAplicaUnidadesApoyo());
        c.setVerificado(false);
        c.setFechaSolicitud(Instant.now());
        c.setFechaLimite(LocalDate.now().plusDays(10));
        c.setEstado("PENDIENTE");
        c.setCreadoEn(Instant.now());
        c.setActualizadoEn(Instant.now());

        List<GuiaHigieneDocumentoJpaEntity> docsMaestros = documentoRepository.findByGuiaIdOrderByCreadoEnAsc(maestro.getId());
        for (GuiaHigieneDocumentoJpaEntity docM : docsMaestros) {
            GuiaHigieneDocumentoJpaEntity docC = new GuiaHigieneDocumentoJpaEntity();
            docC.setId(UUID.randomUUID());
            docC.setGuia(c);
            docC.setTipoDocumentoClave(docM.getTipoDocumentoClave());
            docC.setTipoDocumentoNombre(docM.getTipoDocumentoNombre());
            docC.setOrganismoEmisor(docM.getOrganismoEmisor());
            docC.setNumeroNormativa(docM.getNumeroNormativa());
            docC.setDescripcion(docM.getDescripcion());
            docC.setNombreArchivo(docM.getNombreArchivo());
            docC.setUrlMinio(docM.getUrlMinio());
            docC.setRutaRepositorio(docM.getRutaRepositorio());
            docC.setObligatorio(docM.getObligatorio());
            docC.setCargado(docM.getCargado());
            docC.setFechaSubida(docM.getFechaSubida());
            docC.setCreadoEn(Instant.now());
            c.getDocumentos().add(docC);
        }

        return guiaRepository.save(c);
    }
}
