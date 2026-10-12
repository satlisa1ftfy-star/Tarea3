package com.coresales.service.organizacion.repository;

import com.coresales.service.organizacion.model.UnidadOrganica;
import java.util.List;

public interface UnidadOrganicaRepository {

    List<UnidadOrganica> buscarPorNombre(
            String nombre
    );
}
