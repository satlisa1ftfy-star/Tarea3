package com.coresales.service.product.service;

import com.coresales.service.product.model.Requerimiento;
import com.coresales.service.product.repository.RequerimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RequerimientoServiceImpl
        implements IRequerimientoService {

    private final RequerimientoRepository requerimientoRepository;

    public RequerimientoServiceImpl(
            RequerimientoRepository requerimientoRepository
    ) {
        this.requerimientoRepository =
                requerimientoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Requerimiento> buscar(
            Integer numero,
            String titulo,
            String fechaInicio,
            String fechaFin,
            Integer codigoPersonaSolicitante,
            Integer codigoUoSolicitante,
            Integer codigoPersonaResponsable,
            Integer codigoUoResponsable,
            Integer codigoEstado
    ) {

        // Número de requerimiento
        // 0 = todos
        if (numero == null) {
            numero = 0;
        }

        // Título
        // TODO = todos
        if (titulo == null
                || titulo.trim().isEmpty()) {

            titulo = "TODO";
        }

        // Fecha inicio
        // TODO = no aplicar fecha inicial
        if (fechaInicio == null
                || fechaInicio.trim().isEmpty()) {

            fechaInicio = "TODO";
        }

        // Fecha fin
        // TODO = no aplicar fecha final
        if (fechaFin == null
                || fechaFin.trim().isEmpty()) {

            fechaFin = "TODO";
        }

        // Solicitante
        // 0 = todos
        if (codigoPersonaSolicitante == null) {
            codigoPersonaSolicitante = 0;
        }

        // UO del solicitante
        // 0 = todas
        if (codigoUoSolicitante == null) {
            codigoUoSolicitante = 0;
        }

        // Responsable
        // 0 = todos
        if (codigoPersonaResponsable == null) {
            codigoPersonaResponsable = 0;
        }

        // UO responsable
        // 0 = todas
        if (codigoUoResponsable == null) {
            codigoUoResponsable = 0;
        }

        // Estado
        // 0 = todos
        if (codigoEstado == null) {
            codigoEstado = 0;
        }

        return requerimientoRepository.buscar(
                numero,
                titulo.trim(),
                fechaInicio.trim(),
                fechaFin.trim(),
                codigoPersonaSolicitante,
                codigoUoSolicitante,
                codigoPersonaResponsable,
                codigoUoResponsable,
                codigoEstado
        );
    }
}
