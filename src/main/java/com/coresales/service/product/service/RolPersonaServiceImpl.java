package com.coresales.service.product.service;

import com.coresales.service.product.model.RolPersona;
import com.coresales.service.product.repository.RolPersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RolPersonaServiceImpl
        implements IRolPersonaService {

    private final RolPersonaRepository rolPersonaRepository;

    public RolPersonaServiceImpl(
            RolPersonaRepository rolPersonaRepository
    ) {
        this.rolPersonaRepository =
                rolPersonaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolPersona> listarPorPersona(
            Integer codigoPer
    ) {

        if (codigoPer == null || codigoPer <= 0) {
            throw new IllegalArgumentException(
                    "El código de persona debe ser mayor a 0."
            );
        }

        return rolPersonaRepository
                .listarPorPersona(codigoPer);
    }
}