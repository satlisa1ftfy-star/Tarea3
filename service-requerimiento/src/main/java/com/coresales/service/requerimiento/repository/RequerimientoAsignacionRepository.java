package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.RequerimientoAsignado;

public interface RequerimientoAsignacionRepository {

    RequerimientoAsignado asignar(
            Integer codigoRequerimiento,
            Integer codigoPersonaAsigna,
            String codigoPersonaResponsable,
            boolean responsablePrincipal,
            boolean informeTecnico,
            String observacion,
            String codigoPersonaActualizacion,
            String nombreTerminal
    );
}
