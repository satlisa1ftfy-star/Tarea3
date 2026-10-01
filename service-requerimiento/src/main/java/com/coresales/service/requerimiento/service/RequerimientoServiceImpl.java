package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.config.DocumentoStorage;
import com.coresales.service.requerimiento.model.ClasificarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RegistrarRequerimientoRequest;
import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalleDTO;
import com.coresales.service.requerimiento.model.RequerimientoRegistroResponse;
import com.coresales.service.requerimiento.repository.RequerimientoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class RequerimientoServiceImpl
        implements IRequerimientoService {

    private final RequerimientoRepository requerimientoRepository;

    private final DocumentoStorage documentoStorage;

    public RequerimientoServiceImpl(
            RequerimientoRepository requerimientoRepository,
            DocumentoStorage documentoStorage
    ) {
        this.requerimientoRepository =
                requerimientoRepository;
        this.documentoStorage = documentoStorage;
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
            Integer codigoEstado,
            Integer vigencia
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

        // Vigencia
        // 1 = vigentes, 2 = no vigentes, 3 = todos (por defecto)
        if (vigencia == null) {
            vigencia = 3;
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
                codigoEstado,
                vigencia
        );
    }

    /**
     * Registra el requerimiento (spGR_Requerimiento_Registrar, siTipBus = 1) y, si trae adjunto,
     * mueve el archivo temporal a FileReqN{id}.{ext}. Si el archivo falla, la transacción se revierte.
     */
    @Override
    @Transactional
    public RequerimientoRegistroResponse registrar(
            RegistrarRequerimientoRequest solicitud,
            String ipTerminal
    ) {
        if (solicitud == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Falta el cuerpo de la solicitud.");
        }

        String adjuntoTemporal = null;
        String nombreOriginal = null;
        if (solicitud.getArchivosAdjuntos() != null && !solicitud.getArchivosAdjuntos().isEmpty()) {
            Map<String, Object> adjunto = solicitud.getArchivosAdjuntos().get(0);
            adjuntoTemporal = adjunto.get("archivoTemporalId") == null ? null : adjunto.get("archivoTemporalId").toString();
            nombreOriginal = adjunto.get("nombreOriginal") == null ? null : adjunto.get("nombreOriginal").toString();
            if (!documentoStorage.existeTemporal(adjuntoTemporal)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "El archivo adjunto no existe o expiró; vuelva a adjuntarlo.");
            }
        }

        RequerimientoRegistroResponse respuesta = requerimientoRepository.registrar(solicitud, ipTerminal);

        if (adjuntoTemporal != null) {
            String nombreFinal = "FileReqN" + respuesta.getRequerimientoId() + DocumentoStorage.extension(nombreOriginal);
            try {
                documentoStorage.confirmar(adjuntoTemporal, nombreFinal);
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo guardar el archivo adjunto: " + e.getMessage(), e);
            }
        }
        return respuesta;
    }

    @Override
    @Transactional
    public boolean clasificar(
            ClasificarRequerimientoRequest solicitud,
            String ipTerminal
    ) {
        return requerimientoRepository.clasificar(solicitud, ipTerminal);
    }

    @Override
    @Transactional(readOnly = true)
    public RequerimientoDetalleDTO obtenerParaClasificar(Integer id) {
        return requerimientoRepository.obtenerParaClasificar(id);
    }
}