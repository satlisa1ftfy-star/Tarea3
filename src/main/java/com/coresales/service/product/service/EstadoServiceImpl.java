package com.coresales.service.product.service;

import com.coresales.service.product.model.Estado;
import com.coresales.service.product.repository.EstadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstadoServiceImpl
        implements IEstadoService {

    private final EstadoRepository estadoRepository;

    public EstadoServiceImpl(
            EstadoRepository estadoRepository
    ) {
        this.estadoRepository =
                estadoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Estado> buscar(
            String nombre
    ) {

        if (nombre == null
                || nombre.trim().isEmpty()) {

            nombre = "TODO";
        }

        return estadoRepository
                .buscarPorNombre(
                        nombre.trim()
                );
    }
}
