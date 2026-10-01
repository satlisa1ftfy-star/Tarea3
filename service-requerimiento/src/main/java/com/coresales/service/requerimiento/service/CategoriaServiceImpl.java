package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.CategoriaActivo;
import com.coresales.service.requerimiento.model.Categoria;
import com.coresales.service.requerimiento.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class CategoriaServiceImpl
        implements ICategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaServiceImpl(
            CategoriaRepository categoriaRepository
    ) {
        this.categoriaRepository =
                categoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    ) {

        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "TODO";
        }

        if (codigoUo == null) {
            codigoUo = 0;
        }

        return categoriaRepository.buscar(
                nombre.trim(),
                codigoUo
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaActivo> buscarConActivo(Integer codigoUo) {
        return categoriaRepository.buscarConActivo(codigoUo == null ? 0 : codigoUo);
    }
}
