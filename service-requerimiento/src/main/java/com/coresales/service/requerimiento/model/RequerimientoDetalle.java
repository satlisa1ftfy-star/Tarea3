package com.coresales.service.requerimiento.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ubicacionActual = destino de la última transferencia (o UO de registro si no hubo).
@Data
@NoArgsConstructor
public class RequerimientoDetalle {

    // Datos del requerimiento
    private Integer codigoRequerimiento;
    private Integer codigoRequerimientoOrigen;
    private LocalDateTime fechaRegistro;
    private String estado;
    private String prioridad;
    private String categoria;
    private String subcategoria;
    private String clasificacion;      // Clasificación según procedimiento GIN-PR001
    private String descripcion;        // HTML tal cual lo guarda el legado
    private String observacion;

    // Solicitante
    private String unidadOrganicaSolicitante;
    private String solicitante;
    private String cargoSolicitante;

    // Responsable
    private String asignadoPor;
    private String responsable;
    private Boolean responsablePrincipal;
    private String cargoResponsable;
    private Boolean indicadorAvance;

    // Datos adicionales
    private String ubicacionActual;
    private String tipoEvaluacion;
    private String medioEnvio;
    private String tipoDocumentoReferencia;
    private String documentoReferencia;
    private String fechaDocumentoReferencia;
    private Boolean documentoFisico;

    private List<CorreoCopia> correosCopia = new ArrayList<>();
}
