package com.coresales.service.requerimiento.repository;


import com.coresales.service.requerimiento.model.Persona;

import java.util.List;


public interface PersonaRepository {

    List<Persona> listarPersonas(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}