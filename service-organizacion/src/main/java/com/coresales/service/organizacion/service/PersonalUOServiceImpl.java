package com.coresales.service.organizacion.service;

import com.coresales.service.organizacion.model.PersonalUO;
import com.coresales.service.organizacion.repository.PersonalUORepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PersonalUOServiceImpl
        implements IPersonalUOService {

    private final PersonalUORepository personalUORepository;

    public PersonalUOServiceImpl(
            PersonalUORepository personalUORepository
    ) {
        this.personalUORepository =
                personalUORepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalUO> buscarPersonalxUO(
            String codigoUo,
            Integer tipo
    ) {

        // El SP arma SQL dinámico concatenando @PNCODUO:
        // solo se aceptan dígitos (máx. 10, igual que VARCHAR(10)).
        if (codigoUo == null || !codigoUo.trim().matches("\\d{1,10}")) {
            throw new IllegalArgumentException(
                    "El código de unidad orgánica debe ser numérico (máx. 10 dígitos)."
            );
        }

        if (tipo == null) {
            tipo = 1;
        }

        if (tipo != 1 && tipo != 2) {
            throw new IllegalArgumentException(
                    "El tipo debe ser 1 o 2. " +
                            "1 = Solo la unidad, 2 = Toda la unidad (incluye dependientes)."
            );
        }

        return personalUORepository.buscarPersonalxUO(
                codigoUo.trim(),
                tipo.shortValue()
        );
    }
}
