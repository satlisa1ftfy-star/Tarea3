package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.AsignarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoAsignado;
import com.coresales.service.requerimiento.repository.RequerimientoAsignacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class RequerimientoAsignacionServiceImpl
        implements IRequerimientoAsignacionService {

    private static final int LARGO_MAXIMO_TERMINAL = 20;

    private static final String TERMINAL_DESCONOCIDA = "WEB";

    private final RequerimientoAsignacionRepository requerimientoAsignacionRepository;

    public RequerimientoAsignacionServiceImpl(
            RequerimientoAsignacionRepository requerimientoAsignacionRepository
    ) {
        this.requerimientoAsignacionRepository =
                requerimientoAsignacionRepository;
    }

    @Override
    @Transactional
    public RequerimientoAsignado asignar(
            AsignarRequerimientoRequest solicitud,
            String ipCliente
    ) {

        validar(solicitud);

        String nombreTerminal =
                resolverNombreTerminal(ipCliente);

        return requerimientoAsignacionRepository.asignar(
                solicitud.getCodigoRequerimiento(),
                solicitud.getCodigoPersonaAsigna(),
                solicitud.getCodigoPersonaResponsable().trim(),
                Boolean.TRUE.equals(solicitud.getResponsablePrincipal()),
                Boolean.TRUE.equals(solicitud.getInformeTecnico()),
                solicitud.getObservacion() == null
                        ? ""
                        : solicitud.getObservacion(),
                solicitud.getCodigoPersonaActualizacion().trim(),
                nombreTerminal
        );
    }

    private void validar(AsignarRequerimientoRequest solicitud) {

        if (solicitud == null) {
            throw new IllegalArgumentException(
                    "Los datos de asignación son obligatorios."
            );
        }

        if (solicitud.getCodigoRequerimiento() == null
                || solicitud.getCodigoRequerimiento() <= 0) {
            throw new IllegalArgumentException(
                    "El código de requerimiento debe ser mayor a 0."
            );
        }

        if (solicitud.getCodigoPersonaAsigna() == null
                || solicitud.getCodigoPersonaAsigna() <= 0) {
            throw new IllegalArgumentException(
                    "El código de la persona que asigna (usuario logueado) "
                            + "debe ser mayor a 0."
            );
        }

        if (!StringUtils.hasText(solicitud.getCodigoPersonaResponsable())) {
            throw new IllegalArgumentException(
                    "El código de la persona responsable destino es obligatorio."
            );
        }

        if (!StringUtils.hasText(solicitud.getCodigoPersonaActualizacion())) {
            throw new IllegalArgumentException(
                    "El código de persona de auditoría "
                            + "(codigoPersonaActualizacion) es obligatorio."
            );
        }

        if (solicitud.getObservacion() != null
                && solicitud.getObservacion().length() > 2000) {
            throw new IllegalArgumentException(
                    "La observación no puede superar los 2000 caracteres."
            );
        }
    }

    /**
     * Resuelve el nombre de la PC del cliente vía DNS inverso (cNombreTerminal_Req)
     **/
    private String resolverNombreTerminal(String ipCliente) {

        if (!StringUtils.hasText(ipCliente)) {
            return TERMINAL_DESCONOCIDA;
        }

        try {

            InetAddress direccion =
                    InetAddress.getByName(ipCliente);

            String nombreCanonico =
                    direccion.getCanonicalHostName();

            // Si no hay PTR, getCanonicalHostName() devuelve la IP tal cual
            // (no lanza excepción) - ahí no hay nombre real que usar.
            if (!StringUtils.hasText(nombreCanonico)
                    || nombreCanonico.equals(ipCliente)) {
                return TERMINAL_DESCONOCIDA + "-" + ipCliente;
            }

            // "P7A-ASE05.SAT.GOB.PE" -> "P7A-ASE05" (mismo formato que
            // mandaba la app de escritorio legada).
            String nombreCorto =
                    nombreCanonico
                            .split("\\.")[0]
                            .toUpperCase();

            return nombreCorto.length() > LARGO_MAXIMO_TERMINAL
                    ? nombreCorto.substring(0, LARGO_MAXIMO_TERMINAL)
                    : nombreCorto;

        } catch (UnknownHostException excepcion) {
            return TERMINAL_DESCONOCIDA + "-" + ipCliente;
        }
    }
}
