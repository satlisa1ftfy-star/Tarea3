package com.coresales.service.product.service;

import com.coresales.service.product.model.Categoria;
import java.util.List;


public interface ICategoriaService {

    List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    );
}