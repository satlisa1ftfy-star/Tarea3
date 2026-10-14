package com.coresales.service.requerimiento.service;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.repository.RequerimientoAutorizarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RequerimientoAutorizarServiceImpl
        implements IRequerimientoAutorizarService {

    // Mismo criterio que la asignación: errores de datos como IllegalArgumentException.
    private static final int LARGO_MAXIMO_OBSERVACION = 2000;
    private static final int LARGO_MAXIMO_TERMINAL = 20;

    private final RequerimientoAutorizarRepository requerimientoAutorizarRepository;

    public RequerimientoAutorizarServiceImpl(
            RequerimientoAutorizarRepository requerimientoAutorizarRepository
    ) {
        this.requerimientoAutorizarRepository =
                requerimientoAutorizarRepository;
    }

    @Override
    @Transactional
    public RequerimientoResultado autorizar(
            AutorizarRequerimientoRequest solicitud
    ) {

        validar(solicitud);
        String terminal = solicitud.getNomTerminal();
        if (terminal != null && terminal.length() > LARGO_MAXIMO_TERMINAL) {
            solicitud.setNomTerminal(terminal.substring(0, LARGO_MAXIMO_TERMINAL));
        }
        return  requerimientoAutorizarRepository.autorizar(
                solicitud
        );
    }

    private void validar(AutorizarRequerimientoRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Los datos de la autorización son obligatorios.");
        }
        if (solicitud.getCodRequerimiento() == null || solicitud.getCodRequerimiento() <= 0) {
            throw new IllegalArgumentException("El código de requerimiento debe ser mayor a 0.");
        }
        if (solicitud.getCodPerHelpDesk() == null || solicitud.getCodPerHelpDesk() <= 0) {
            throw new IllegalArgumentException("El código de la persona que autoriza debe ser mayor a 0.");
        }
        if (!StringUtils.hasText(solicitud.getCodPerActualiza())) {
            throw new IllegalArgumentException("El código de persona de auditoría (codPerActualiza) es obligatorio.");
        }
        if (solicitud.getObservacion() != null
                && solicitud.getObservacion().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new IllegalArgumentException("La observación no puede superar los 2000 caracteres.");
        }
    }
}
