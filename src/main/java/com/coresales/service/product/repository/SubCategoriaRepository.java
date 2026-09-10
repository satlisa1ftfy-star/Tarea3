package com.coresales.service.product.repository;

import com.coresales.service.product.model.SubCategoria;

import java.util.List;

public interface SubCategoriaRepository {

    List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    );
}