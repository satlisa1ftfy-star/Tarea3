package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.DocumentoAdjuntoDetalle;
import com.coresales.service.requerimiento.model.HistorialRequerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalle;
import com.coresales.service.requerimiento.repository.RequerimientoDetalleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RequerimientoDetalleServiceImpl
        implements IRequerimientoDetalleService {

    private final RequerimientoDetalleRepository requerimientoDetalleRepository;

    public RequerimientoDetalleServiceImpl(
            RequerimientoDetalleRepository requerimientoDetalleRepository
    ) {
        this.requerimientoDetalleRepository =
                requerimientoDetalleRepository;
    }

    @Override
    public RequerimientoDetalle consultarDetalle(Integer codigoRequerimiento) {
        validar(codigoRequerimiento);
        return requerimientoDetalleRepository.consultarDetalle(codigoRequerimiento);
    }

    @Override
    public List<HistorialRequerimiento> consultarHistorial(Integer codigoRequerimiento) {
        validar(codigoRequerimiento);
        return requerimientoDetalleRepository.consultarHistorial(codigoRequerimiento);
    }

    @Override
    public List<DocumentoAdjuntoDetalle> consultarAdjuntos(Integer codigoRequerimiento) {
        validar(codigoRequerimiento);
        return requerimientoDetalleRepository.consultarAdjuntos(codigoRequerimiento);
    }

    private void validar(Integer codigoRequerimiento) {
        if (codigoRequerimiento == null || codigoRequerimiento <= 0) {
            throw new IllegalArgumentException("El código de requerimiento es obligatorio.");
        }
    }
}
