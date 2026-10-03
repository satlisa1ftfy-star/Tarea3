package com.coresales.service.organizacion.repository;

import com.coresales.service.organizacion.model.PersonalUO;

import java.util.List;

public interface PersonalUORepository {

    List<PersonalUO> buscarPersonalxUO(
            String codigoUo,
            Short tipo
    );
}
