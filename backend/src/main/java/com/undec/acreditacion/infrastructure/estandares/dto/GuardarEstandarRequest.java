package com.undec.acreditacion.infrastructure.estandares.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;

import java.util.List;
import java.util.UUID;

public record GuardarEstandarRequest(
        UUID carreraId,
        String nombre,
        String resolucionMinisterial,
        Integer anioVigencia,
        EstadoVigenciaEstandar estadoVigencia,
        TipoSeccionEstandar22 tipoSeccion22,
        TipoEvaluacionEstandar22 tipoEvaluacion22,
        String instrucciones22,
        String observaciones,
        Boolean habilitar311,
        ModoSeccion311 modo311,
        Integer horasMinimasTotal311,
        Boolean habilitar312,
        ModoSeccion312 modo312,
        Integer horasMinimasTotal312,
        List<EscalaItemRequest> escalas,
        List<Grupo22Request> grupos22,
        List<AreaCargaHorariaRequest> areasCargaHoraria
) {
    public GuardarEstandarRequest(
            UUID carreraId,
            String nombre,
            String resolucionMinisterial,
            Integer anioVigencia,
            EstadoVigenciaEstandar estadoVigencia,
            TipoSeccionEstandar22 tipoSeccion22,
            TipoEvaluacionEstandar22 tipoEvaluacion22,
            String instrucciones22,
            String observaciones,
            List<EscalaItemRequest> escalas,
            List<Grupo22Request> grupos22,
            List<AreaCargaHorariaRequest> areasCargaHoraria
    ) {
        this(
                carreraId,
                nombre,
                resolucionMinisterial,
                anioVigencia,
                estadoVigencia,
                tipoSeccion22,
                tipoEvaluacion22,
                instrucciones22,
                observaciones,
                true,
                tipoSeccion22 == TipoSeccionEstandar22.EJES
                        ? ModoSeccion311.BLOQUES_CURRICULARES
                        : ModoSeccion311.AREAS_TEMATICAS,
                null,
                true,
                tipoSeccion22 == TipoSeccionEstandar22.EJES
                        ? ModoSeccion312.MODALIDADES_PRACTICAS
                        : ModoSeccion312.AREAS_PRACTICAS,
                null,
                escalas,
                grupos22,
                areasCargaHoraria
        );
    }

    public record EscalaItemRequest(
            Integer orden,
            String codigo,
            String etiqueta,
            String descripcionReferencia,
            Boolean esValorPorDefecto
    ) {}

    public record Item22Request(
            Integer orden,
            String enunciado
    ) {}

    public record Grupo22Request(
            Integer orden,
            String codigo,
            String titulo,
            String descripcion,
            Boolean habilitado,
            List<Item22Request> items
    ) {}

    public record AreaCargaHorariaRequest(
            TipoSeccionCargaHoraria tipoSeccion,
            Integer orden,
            String codigo,
            String nombre,
            String etiquetaPractica,
            Boolean habilitada,
            Integer horasMinimasCarrera
    ) {
        public AreaCargaHorariaRequest(
                TipoSeccionCargaHoraria tipoSeccion,
                Integer orden,
                String codigo,
                String nombre,
                String etiquetaPractica,
                Boolean habilitada
        ) {
            this(tipoSeccion, orden, codigo, nombre, etiquetaPractica, habilitada, null);
        }

        public AreaCargaHorariaRequest(
                Integer orden,
                String codigo,
                String nombre,
                String etiquetaPractica,
                Boolean habilitada
        ) {
            this(TipoSeccionCargaHoraria.CARGA_TOTAL_311, orden, codigo, nombre, etiquetaPractica, habilitada, null);
        }
    }
}
