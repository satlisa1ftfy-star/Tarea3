package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.Requerimiento;

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
}