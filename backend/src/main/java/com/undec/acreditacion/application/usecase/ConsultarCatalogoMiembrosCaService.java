package com.undec.acreditacion.application.usecase;

import com.undec.acreditacion.application.input.ConsultarCatalogoMiembrosCaUseCase;
import com.undec.acreditacion.application.output.CatalogoMiembroComisionDto;
import com.undec.acreditacion.domain.ports.CatalogoMiembroComisionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarCatalogoMiembrosCaService implements ConsultarCatalogoMiembrosCaUseCase {

    private final CatalogoMiembroComisionRepositoryPort catalogoRepository;

    public ConsultarCatalogoMiembrosCaService(CatalogoMiembroComisionRepositoryPort catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMiembroComisionDto> buscar(String busqueda) {
        return catalogoRepository.buscar(busqueda).stream()
                .map(m -> new CatalogoMiembroComisionDto(
                        m.getId(),
                        m.getApellido(),
                        m.getNombreCompleto(),
                        m.getDocumento(),
                        m.getCuitPasaporte(),
                        m.getCorreoElectronico(),
                        m.getCorreoGmail(),
                        m.getCargoFuncion(),
                        m.getCreadoEn()
                ))
                .toList();
    }
}
