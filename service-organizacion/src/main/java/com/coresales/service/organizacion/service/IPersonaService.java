package com.coresales.service.organizacion.service;


import com.coresales.service.organizacion.model.Persona;

import java.util.List;


public interface IPersonaService {

    List<Persona> listar(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}