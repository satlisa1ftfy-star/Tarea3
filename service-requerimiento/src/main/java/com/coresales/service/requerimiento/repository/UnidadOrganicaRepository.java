package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.UnidadOrganica;
import java.util.List;

public interface UnidadOrganicaRepository {

    List<UnidadOrganica> buscarPorNombre(
            String nombre
    );
}
