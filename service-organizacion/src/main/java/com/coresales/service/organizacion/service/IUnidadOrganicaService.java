package com.coresales.service.organizacion.service;


import com.coresales.service.organizacion.model.UnidadOrganica;

import java.util.List;

public interface IUnidadOrganicaService {

    List<UnidadOrganica> buscar(
            String nombre
    );

    List<UnidadOrganica> listarParaSolicitud();

    List<UnidadOrganica> buscarDependencias(Integer codigoUo);
}