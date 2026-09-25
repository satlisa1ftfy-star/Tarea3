package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;

public interface IRequerimientoAutorizarService {

    RequerimientoResultado autorizar(
            AutorizarRequerimientoRequest solicitud
    );
}
