package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.dto.response.CargoResponse;
import com.coresales.service.requerimiento.model.Categoria;
import java.util.List;


public interface ICargoService {

    List<CargoResponse> buscar(
            Integer tipBus,
            Integer codigoReq,
            Integer codigoCat,
            Integer codigo_SubCat
    );
}