package com.coresales.service.requerimiento.service;


import com.coresales.service.requerimiento.model.UnidadOrganica;

import java.util.List;

public interface IUnidadOrganicaService {

    List<UnidadOrganica> buscar(
            String nombre
    );
}