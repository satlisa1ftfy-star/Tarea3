package com.coresales.service.requerimiento.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequerimientoDetalleDTO {

    @JsonProperty("requerimientoId")
    private Integer requerimientoId;

    @JsonProperty("numeroRequerimiento")
    private String numeroRequerimiento;

    @JsonProperty("fechaRequerimiento")
    private String fechaRequerimiento;

    @JsonProperty("estadoActual")
    private String estadoActual;

    @JsonProperty("codigoEstado")
    private Integer codigoEstado;

    @JsonProperty("solicitanteNombreCompleto")
    private String solicitanteNombreCompleto;

    @JsonProperty("solicitanteCodigoPersonal")
    private String solicitanteCodigoPersonal;

    @JsonProperty("unidadOrganicaSolicitante")
    private String unidadOrganicaSolicitante;

    @JsonProperty("cargoSolicitante")
    private String cargoSolicitante;

    @JsonProperty("sumilla")
    private String sumilla;

    @JsonProperty("descripcionHtml")
    private String descripcionHtml;

    @JsonProperty("unidadOrganicaDestinoId")
    private Integer unidadOrganicaDestinoId;

    @JsonProperty("activoId")
    private Integer activoId;

    @JsonProperty("categoriaId")
    private Integer categoriaId;

    @JsonProperty("subCategoriaId")
    private Integer subCategoriaId;

    @JsonProperty("prioridadId")
    private Integer prioridadId;

    @JsonProperty("observacion")
    private String observacion;

    @JsonProperty("categoriaNombre")
    private String categoriaNombre;

    @JsonProperty("subCategoriaNombre")
    private String subCategoriaNombre;

    @JsonProperty("prioridadNombre")
    private String prioridadNombre;

    @JsonProperty("unidadOrganicaDestinoNombre")
    private String unidadOrganicaDestinoNombre;

    @JsonProperty("datosComplementarios")
    private List<Map<String, Object>> datosComplementarios = new ArrayList<>();

    @JsonProperty("documentosAdjuntos")
    private List<Map<String, Object>> documentosAdjuntos = new ArrayList<>();
}
