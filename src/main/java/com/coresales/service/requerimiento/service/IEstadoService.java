package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.Estado;

import java.util.List;

public interface IEstadoService {

    List<Estado> buscar(String nombre);
}
