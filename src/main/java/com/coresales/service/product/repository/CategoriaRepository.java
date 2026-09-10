package com.coresales.service.product.repository;

import com.coresales.service.product.model.Categoria;

import java.util.List;

public interface CategoriaRepository {

    List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    );
}