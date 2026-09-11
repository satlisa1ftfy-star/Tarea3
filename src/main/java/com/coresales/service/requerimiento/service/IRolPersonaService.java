package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.RolPersona;
import java.util.List;

public interface IRolPersonaService {

    List<RolPersona> listarPorPersona(
            Integer codigoPer
    );
}