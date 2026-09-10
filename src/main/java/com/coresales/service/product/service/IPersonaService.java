package com.coresales.service.product.service;


import com.coresales.service.product.model.Persona;

import java.util.List;


public interface IPersonaService {

    List<Persona> listar(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    );
}