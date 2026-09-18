package com.coresales.service.requerimiento.service;


import com.coresales.service.requerimiento.model.Persona;

import java.util.List;


public interface IPersonaService {

    List<Persona> listar(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}