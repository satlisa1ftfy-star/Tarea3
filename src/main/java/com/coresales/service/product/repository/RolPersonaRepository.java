package com.coresales.service.product.repository;

import com.coresales.service.product.model.RolPersona;

import java.util.List;

public interface RolPersonaRepository {

    List<RolPersona> listarPorPersona(
            Integer codigoPer
    );
}