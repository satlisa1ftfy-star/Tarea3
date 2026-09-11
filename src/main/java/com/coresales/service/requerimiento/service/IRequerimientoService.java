package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.Requerimiento;

import java.util.List;

public interface IRequerimientoService {

    List<Requerimiento> buscar(
            Integer numero,
            String titulo,
            String fechaInicio,
            String fechaFin,
            Integer codigoPersonaSolicitante,
            Integer codigoUoSolicitante,
            Integer codigoPersonaResponsable,
            Integer codigoUoResponsable,
            Integer codigoEstado
    );
}
