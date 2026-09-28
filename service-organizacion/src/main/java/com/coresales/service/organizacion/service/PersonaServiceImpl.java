package com.coresales.service.organizacion.service;
import com.coresales.service.organizacion.model.Persona;
import com.coresales.service.organizacion.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;




@Service
public class PersonaServiceImpl
        implements IPersonaService {

    private final PersonaRepository personaRepository;

    public PersonaServiceImpl(
            PersonaRepository personaRepository
    ) {
        this.personaRepository =
                personaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Persona> listar(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    ) {

        // Nombre vacío = todos
        if (nombre == null || nombre.trim().isEmpty()) {
            nombre = "TODO";
        }

        // UO vacía = todas
        if (codigoUo == null) {
            codigoUo = 0;
        }

        // Vigencia vacía = todos
        if (vigencia == null) {
            vigencia = 3;
        }

        // Solo se permiten 1, 2 o 3
        if (vigencia < 1 || vigencia > 3) {
            throw new IllegalArgumentException(
                    "La vigencia debe ser 1, 2 o 3. " +
                            "1 = Vigente, 2 = No Vigente, 3 = Todos."
            );
        }

        return personaRepository.listarPersonas(
                nombre.trim(),
                codigoUo,
                vigencia
        );
    }
}