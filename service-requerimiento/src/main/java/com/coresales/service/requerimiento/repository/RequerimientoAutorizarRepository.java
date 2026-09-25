package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;

public interface RequerimientoAutorizarRepository {
    RequerimientoResultado autorizar(
            AutorizarRequerimientoRequest solicitud
    );
}

