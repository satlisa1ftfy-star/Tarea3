package com.coresales.service.requerimiento.service;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;
import com.coresales.service.requerimiento.repository.RequerimientoAutorizarRepository;
import com.coresales.service.requerimiento.repository.RequerimientoSolicitaAutorizaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequerimientoSolicitaAutorizaServiceImpl
        implements IRequerimientoSolicitaAutorizaService {

    private final RequerimientoSolicitaAutorizaRepository requerimientoSolicitaAutorizaRepository;

    public RequerimientoSolicitaAutorizaServiceImpl(
            RequerimientoSolicitaAutorizaRepository requerimientoSolicitaAutorizaRepository
    ) {
        this.requerimientoSolicitaAutorizaRepository =
                requerimientoSolicitaAutorizaRepository;
    }

    @Override
    @Transactional
    public RequerimientoResultado solicitarAutorizacion(
            SolicitarAutorizaRequerimientoRequest solicitud
    ) {

        //validar(solicitud);
        return  requerimientoSolicitaAutorizaRepository.solicitarAutorizacion(
                solicitud
        );
    }

    //private void validar(AutorizarRequerimientoRequest solicitud) {
    //}
}
