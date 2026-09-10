package com.coresales.service.product.repository;

import com.coresales.service.product.model.UnidadOrganica;
import java.util.List;

public interface UnidadOrganicaRepository {

    List<UnidadOrganica> buscarPorNombre(
            String nombre
    );
}
