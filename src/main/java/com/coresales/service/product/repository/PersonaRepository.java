package com.coresales.service.product.repository;


import com.coresales.service.product.model.Persona;

import java.util.List;


public interface PersonaRepository {

    List<Persona> listarPersonas(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}