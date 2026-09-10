package com.coresales.service.product.service;

import com.coresales.service.product.model.SubCategoria;

import java.util.List;

public interface ISubCategoriaService {

    List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    );
}