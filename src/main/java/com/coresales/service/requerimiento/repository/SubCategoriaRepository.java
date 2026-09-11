package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.SubCategoria;

import java.util.List;

public interface SubCategoriaRepository {

    List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    );
}