package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.dto.response.CargoResponse;
import com.coresales.service.requerimiento.model.Categoria;
import com.coresales.service.requerimiento.repository.CargoRepository;
import com.coresales.service.requerimiento.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class CargoServiceImpl
        implements ICargoService {

    private final CargoRepository cargoRepository;

    public CargoServiceImpl(
            CargoRepository cargoRepository
    ) {
        this.cargoRepository =
                cargoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CargoResponse> buscar(
            Integer tipBus,
            Integer codigoReq,
            Integer codigoCat,
            Integer codigo_SubCat
    ) {
        return cargoRepository.buscar(
                tipBus,
                codigoReq,
                codigoCat,
                codigo_SubCat
        );
    }
}