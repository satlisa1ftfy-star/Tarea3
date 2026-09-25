package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;

public interface RequerimientoSolicitaAutorizaRepository {
    RequerimientoResultado solicitarAutorizacion(
            SolicitarAutorizaRequerimientoRequest solicitud
    );
}

