package com.coresales.service.organizacion.service;

import com.coresales.service.organizacion.model.UnidadOrganica;
import com.coresales.service.organizacion.repository.UnidadOrganicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UnidadOrganicaServiceImpl
        implements IUnidadOrganicaService {

    private final UnidadOrganicaRepository unidadOrganicaRepository;

    public UnidadOrganicaServiceImpl(
            UnidadOrganicaRepository unidadOrganicaRepository
    ) {
        this.unidadOrganicaRepository =
                unidadOrganicaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadOrganica> buscar(
            String nombre
    ) {

        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "TODO";
        }

        return unidadOrganicaRepository
                .buscarPorNombre(
                        nombre.trim()
                );
    }
}