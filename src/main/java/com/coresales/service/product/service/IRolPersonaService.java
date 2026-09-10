package com.coresales.service.product.service;

import com.coresales.service.product.model.RolPersona;
import java.util.List;

public interface IRolPersonaService {

    List<RolPersona> listarPorPersona(
            Integer codigoPer
    );
}