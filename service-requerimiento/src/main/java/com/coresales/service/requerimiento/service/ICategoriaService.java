package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.Categoria;
import java.util.List;


public interface ICategoriaService {

    List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    );
}