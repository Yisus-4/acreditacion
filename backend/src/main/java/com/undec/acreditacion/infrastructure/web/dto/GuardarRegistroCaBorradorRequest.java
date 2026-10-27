package com.undec.acreditacion.infrastructure.web.dto;

import com.undec.acreditacion.application.input.MiembroCaInputDto;

import java.util.List;

public record GuardarRegistroCaBorradorRequest(
        boolean coincideCurricular,
        List<MiembroCaInputDto> miembros
) {}
