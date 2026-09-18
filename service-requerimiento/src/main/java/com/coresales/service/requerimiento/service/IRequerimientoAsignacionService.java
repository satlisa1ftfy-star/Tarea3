package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.AsignarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoAsignado;

public interface IRequerimientoAsignacionService {

    RequerimientoAsignado asignar(
            AsignarRequerimientoRequest solicitud,
            String ipCliente
    );
}
