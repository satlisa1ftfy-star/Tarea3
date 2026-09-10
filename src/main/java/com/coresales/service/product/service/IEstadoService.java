package com.coresales.service.product.service;

import com.coresales.service.product.model.Estado;

import java.util.List;

public interface IEstadoService {

    List<Estado> buscar(String nombre);
}
