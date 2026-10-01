package com.coresales.service.requerimiento.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClasificarRequerimientoRequest {

    @Schema(description = "Código del requerimiento a clasificar", example = "104589")
    @JsonProperty("requerimientoId")
    @JsonAlias({"codigoRequerimiento", "iCodigo_Req"})
    private Integer requerimientoId;

    @Schema(description = "Código de la Unidad Orgánica destino / reasignada", example = "175")
    @JsonProperty("unidadOrganicaId")
    @JsonAlias({"codigoUo", "iCodigo_Uo"})
    private Integer unidadOrganicaId;

    @Schema(description = "Código del tipo de activo (opcional)", example = "1")
    @JsonProperty("activoId")
    @JsonAlias({"codigoActivo", "siCodigo_Act"})
    private Integer activoId;

    @Schema(description = "Código de la categoría seleccionada", example = "100")
    @JsonProperty("categoriaId")
    @JsonAlias({"codigoCategoria", "siCodigo_Cat"})
    private Integer categoriaId;

    @Schema(description = "Código de la subcategoría seleccionada", example = "90")
    @JsonProperty("subCategoriaId")
    @JsonAlias({"codigoSubcategoria", "siCodigo_SubCat"})
    private Integer subCategoriaId;

    @Schema(description = "Código de la prioridad asignada (ej: 31=Alta, 32=Media, 33=Baja)", example = "32")
    @JsonProperty("prioridadId")
    @JsonAlias({"codigoPrioridad", "iCodigo_Pri"})
    private Integer prioridadId;

    @Schema(description = "Observaciones de la clasificación técnica", example = "Clasificado para atención de soporte.")
    @JsonProperty("observacion")
    @JsonAlias({"vObservacion_Req"})
    private String observacion;

    @Schema(description = "Indica si se solicita autorización de jefatura", example = "false")
    @JsonProperty("solicitaAutorizacion")
    private Boolean solicitaAutorizacion;

    @Schema(description = "Motivo de la solicitud de autorización (obligatorio si solicitaAutorizacion=true)")
    @JsonProperty("motivoSolicitudAutorizacion")
    private String motivoSolicitudAutorizacion;

    @Schema(description = "IDs de personas autorizadoras")
    @JsonProperty("personasAutorizacionIds")
    private List<Integer> personasAutorizacionIds;

    @Schema(description = "Código de persona de auditoría", example = "1309")
    @JsonProperty("codigoPersonaActualizacion")
    @JsonAlias({"cCodPerActualizacion"})
    private String codigoPersonaActualizacion;
}
