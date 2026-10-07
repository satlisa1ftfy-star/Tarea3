package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.dto.response.ConsultaSolicitudAutorizaResponse;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;

import java.util.List;

public interface RequerimientoSolicitaAutorizaRepository {
    RequerimientoResultado solicitarAutorizacion(
            SolicitarAutorizaRequerimientoRequest solicitud
    );
    List<ConsultaSolicitudAutorizaResponse> consultarSolicitud(
            String tipBus,
            Integer codigoRequerimiento
    );
}

