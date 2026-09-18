package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.Categoria;

import java.util.List;

public interface CategoriaRepository {

    List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    );
}