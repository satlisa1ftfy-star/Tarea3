package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.ClasificarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RegistrarRequerimientoRequest;
import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalleDTO;
import com.coresales.service.requerimiento.model.RequerimientoRegistroResponse;

import java.util.List;


public interface RequerimientoRepository {

    List<Requerimiento> buscar(
            Integer numero,
            String titulo,
            String fechaInicio,
            String fechaFin,
            Integer codigoPersonaSolicitante,
            Integer codigoUoSolicitante,
            Integer codigoPersonaResponsable,
            Integer codigoUoResponsable,
            Integer codigoEstado,
            Integer vigencia
    );

    RequerimientoRegistroResponse registrar(RegistrarRequerimientoRequest solicitud, String ipTerminal);

    boolean clasificar(ClasificarRequerimientoRequest solicitud, String ipTerminal);

    RequerimientoDetalleDTO obtenerParaClasificar(Integer id);
}