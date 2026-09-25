package com.coresales.service.requerimiento.service;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.repository.RequerimientoAutorizarRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequerimientoAutorizarServiceImpl
        implements IRequerimientoAutorizarService {


    private final RequerimientoAutorizarRepository requerimientoAutorizarRepository;

    public RequerimientoAutorizarServiceImpl(
            RequerimientoAutorizarRepository requerimientoAutorizarRepository
    ) {
        this.requerimientoAutorizarRepository =
                requerimientoAutorizarRepository;
    }

    @Override
    @Transactional
    public RequerimientoResultado autorizar(
            AutorizarRequerimientoRequest solicitud
    ) {

        validar(solicitud);
        return  requerimientoAutorizarRepository.autorizar(
                solicitud
        );
    }

    private void validar(AutorizarRequerimientoRequest solicitud) {
    }
}
