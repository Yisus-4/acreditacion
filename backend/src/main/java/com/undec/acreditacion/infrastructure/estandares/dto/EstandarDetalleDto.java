package com.undec.acreditacion.infrastructure.estandares.dto;

import com.undec.acreditacion.domain.entities.EstadoVigenciaEstandar;
import com.undec.acreditacion.domain.entities.ModoSeccion311;
import com.undec.acreditacion.domain.entities.ModoSeccion312;
import com.undec.acreditacion.domain.entities.TipoEvaluacionEstandar22;
import com.undec.acreditacion.domain.entities.TipoSeccionCargaHoraria;
import com.undec.acreditacion.domain.entities.TipoSeccionEstandar22;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EstandarDetalleDto(
        UUID id,
        UUID carreraId,
        String carreraNombre,
        String carreraSigla,
        String nombre,
        String resolucionMinisterial,
        Integer anioVigencia,
        EstadoVigenciaEstandar estadoVigencia,
        TipoSeccionEstandar22 tipoSeccion22,
        TipoEvaluacionEstandar22 tipoEvaluacion22,
        String instrucciones22,
        String observaciones,
        boolean habilitar311,
        ModoSeccion311 modo311,
        Integer horasMinimasTotal311,
        boolean habilitar312,
        ModoSeccion312 modo312,
        Integer horasMinimasTotal312,
        long planesAsociados,
        boolean editable,
        List<EscalaDto> escalas,
        List<Grupo22Dto> grupos22,
        List<AreaCargaHorariaDto> areasCargaHoraria,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public EstandarDetalleDto(
            UUID id,
            UUID carreraId,
            String carreraNombre,
            String carreraSigla,
            String nombre,
            String resolucionMinisterial,
            Integer anioVigencia,
            EstadoVigenciaEstandar estadoVigencia,
            TipoSeccionEstandar22 tipoSeccion22,
            TipoEvaluacionEstandar22 tipoEvaluacion22,
            String instrucciones22,
            String observaciones,
            long planesAsociados,
            boolean editable,
            List<EscalaDto> escalas,
            List<Grupo22Dto> grupos22,
            List<AreaCargaHorariaDto> areasCargaHoraria,
            Instant creadoEn,
            Instant actualizadoEn
    ) {
        this(
                id,
                carreraId,
                carreraNombre,
                carreraSigla,
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
                planesAsociados,
                editable,
                escalas,
                grupos22,
                areasCargaHoraria,
                creadoEn,
                actualizadoEn
        );
    }

    public record EscalaDto(
            UUID id,
            Integer orden,
            String codigo,
            String etiqueta,
            String descripcionReferencia,
            boolean esValorPorDefecto
    ) {}

    public record Item22Dto(
            UUID id,
            Integer orden,
            String enunciado
    ) {}

    public record Grupo22Dto(
            UUID id,
            Integer orden,
            String codigo,
            String titulo,
            String descripcion,
            boolean habilitado,
            List<Item22Dto> items
    ) {}

    public record AreaCargaHorariaDto(
            UUID id,
            TipoSeccionCargaHoraria tipoSeccion,
            Integer orden,
            String codigo,
            String nombre,
            String etiquetaPractica,
            boolean habilitada,
            Integer horasMinimasCarrera
    ) {
        public AreaCargaHorariaDto(
                UUID id,
                TipoSeccionCargaHoraria tipoSeccion,
                Integer orden,
                String codigo,
                String nombre,
                String etiquetaPractica,
                boolean habilitada
        ) {
            this(id, tipoSeccion, orden, codigo, nombre, etiquetaPractica, habilitada, null);
        }

        public AreaCargaHorariaDto(
                UUID id,
                Integer orden,
                String codigo,
                String nombre,
                String etiquetaPractica,
                boolean habilitada
        ) {
            this(id, TipoSeccionCargaHoraria.CARGA_TOTAL_311, orden, codigo, nombre, etiquetaPractica, habilitada, null);
        }
    }
}
