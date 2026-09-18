package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.Estado;

import java.util.List;

public interface EstadoRepository {

    List<Estado> buscarPorNombre(String nombre);
}