package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.RolPersona;

import java.util.List;

public interface RolPersonaRepository {

    List<RolPersona> listarPorPersona(
            Integer codigoPer
    );
}