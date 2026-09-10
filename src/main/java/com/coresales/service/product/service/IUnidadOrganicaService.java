package com.coresales.service.product.service;


import com.coresales.service.product.model.UnidadOrganica;

import java.util.List;

public interface IUnidadOrganicaService {

    List<UnidadOrganica> buscar(
            String nombre
    );
}