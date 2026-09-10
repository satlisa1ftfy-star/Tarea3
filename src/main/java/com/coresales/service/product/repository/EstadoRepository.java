package com.coresales.service.product.repository;

import com.coresales.service.product.model.Estado;

import java.util.List;

public interface EstadoRepository {

    List<Estado> buscarPorNombre(String nombre);
}