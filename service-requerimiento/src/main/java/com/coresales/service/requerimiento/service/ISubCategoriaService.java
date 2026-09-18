package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.SubCategoria;

import java.util.List;

public interface ISubCategoriaService {

    List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    );
}