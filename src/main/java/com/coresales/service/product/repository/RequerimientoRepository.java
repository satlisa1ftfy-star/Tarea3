package com.coresales.service.product.repository;

import com.coresales.service.product.model.Requerimiento;

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
            Integer codigoEstado
    );
}