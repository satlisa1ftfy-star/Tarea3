package com.coresales.service.organizacion.service;

import com.coresales.service.organizacion.model.PersonalUO;

import java.util.List;

public interface IPersonalUOService {

    List<PersonalUO> buscarPersonalxUO(
            String codigoUo,
            Integer tipo
    );
}
