package com.coresales.service.requerimiento.service;
import com.coresales.service.requerimiento.dto.response.ConsultaSolicitudAutorizaResponse;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;
import com.coresales.service.requerimiento.repository.RequerimientoSolicitaAutorizaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class RequerimientoSolicitaAutorizaServiceImpl
        implements IRequerimientoSolicitaAutorizaService {

    // Mismo criterio que la asignación: errores de datos como IllegalArgumentException.
    private static final int LARGO_MAXIMO_OBSERVACION = 2000;
    private static final int LARGO_MAXIMO_TERMINAL = 20;

    // cCodPer separados por coma: el SP arma SQL dinámico con este valor.
    private static final String PATRON_COD_PER = "\\d{1,4}(,\\d{1,4})*";

    private final RequerimientoSolicitaAutorizaRepository requerimientoSolicitaAutorizaRepository;

    public RequerimientoSolicitaAutorizaServiceImpl(
            RequerimientoSolicitaAutorizaRepository requerimientoSolicitaAutorizaRepository
    ) {
        this.requerimientoSolicitaAutorizaRepository =
                requerimientoSolicitaAutorizaRepository;
    }

    @Override
    @Transactional
    public RequerimientoResultado solicitarAutorizacion(
            SolicitarAutorizaRequerimientoRequest solicitud
    ) {
        validar(solicitud);
        solicitud.setCodPerAutoriza(solicitud.getCodPerAutoriza().replace(" ", ""));
        solicitud.setNomTerminal(recortar(solicitud.getNomTerminal(), LARGO_MAXIMO_TERMINAL));
        return  requerimientoSolicitaAutorizaRepository.solicitarAutorizacion(
                solicitud
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConsultaSolicitudAutorizaResponse> consultarSolicitud(
            String tipBus, Integer codigoRequerimiento
    ) {
        if (codigoRequerimiento == null || codigoRequerimiento <= 0) {
            throw new IllegalArgumentException("El código de requerimiento debe ser mayor a 0.");
        }
        return  requerimientoSolicitaAutorizaRepository.consultarSolicitud(
                tipBus, codigoRequerimiento
        );
    }

    private void validar(SolicitarAutorizaRequerimientoRequest solicitud) {
        if (solicitud == null) {
            throw new IllegalArgumentException("Los datos de la solicitud son obligatorios.");
        }
        if (solicitud.getCodRequerimiento() == null || solicitud.getCodRequerimiento() <= 0) {
            throw new IllegalArgumentException("El código de requerimiento debe ser mayor a 0.");
        }
        if (!StringUtils.hasText(solicitud.getCodPerAutoriza())
                || !solicitud.getCodPerAutoriza().replace(" ", "").matches(PATRON_COD_PER)) {
            throw new IllegalArgumentException(
                    "Las personas que autorizan son obligatorias (cCodPer separados por coma).");
        }
        if (!StringUtils.hasText(solicitud.getCodPerSolAutorizacion())) {
            throw new IllegalArgumentException(
                    "El código de la persona que solicita la autorización es obligatorio.");
        }
        if (solicitud.getCodPerHelpDesk() == null || solicitud.getCodPerHelpDesk() <= 0) {
            throw new IllegalArgumentException("El código de la persona de helpdesk debe ser mayor a 0.");
        }
        if (!StringUtils.hasText(solicitud.getCodPerActualiza())) {
            throw new IllegalArgumentException("El código de persona de auditoría (codPerActualiza) es obligatorio.");
        }
        if (!StringUtils.hasText(solicitud.getObservacion())) {
            throw new IllegalArgumentException("La observación es obligatoria.");
        }
        if (solicitud.getObservacion().length() > LARGO_MAXIMO_OBSERVACION) {
            throw new IllegalArgumentException("La observación no puede superar los 2000 caracteres.");
        }
    }

    private static String recortar(String valor, int largo) {
        return valor != null && valor.length() > largo ? valor.substring(0, largo) : valor;
    }
}
