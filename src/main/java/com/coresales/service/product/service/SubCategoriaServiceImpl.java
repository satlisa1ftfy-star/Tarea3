package com.coresales.service.product.service;

import com.coresales.service.product.model.SubCategoria;
import com.coresales.service.product.repository.SubCategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubCategoriaServiceImpl
        implements ISubCategoriaService {

    private final SubCategoriaRepository subCategoriaRepository;

    public SubCategoriaServiceImpl(
            SubCategoriaRepository subCategoriaRepository
    ) {
        this.subCategoriaRepository =
                subCategoriaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    ) {

        if (codigoCategoria == null) {
            codigoCategoria = 0;
        }

        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "TODO";
        }

        return subCategoriaRepository.buscar(
                codigoCategoria,
                nombre.trim()
        );
    }
}