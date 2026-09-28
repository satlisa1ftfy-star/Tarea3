package com.coresales.service.organizacion.repository;


import com.coresales.service.organizacion.model.Persona;

import java.util.List;


public interface PersonaRepository {

    List<Persona> listarPersonas(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}