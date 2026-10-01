package com.coresales.service.requerimiento.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarRequerimientoRequest {

    @Schema(description = "Código de la Unidad Orgánica destino / que resuelve", example = "175")
    @JsonProperty("unidadOrganicaId")
    @JsonAlias({"codigoUo", "iCodigo_Uo"})
    private Integer unidadOrganicaId;

    @Schema(description = "iCodUO de la división responsable (opcional). Se usa cuando la gerencia no es GI.", example = "176")
    @JsonProperty("divisionId")
    @JsonAlias({"iCodDivRes"})
    private Integer divisionId;

    @Schema(description = "Código del activo de la categoría elegida (siCodigo_Act)", example = "8")
    @JsonProperty("activoId")
    @JsonAlias({"siCodigo_Act"})
    private Integer activoId;

    @Schema(description = "Código de la Categoría", example = "100")
    @JsonProperty("categoriaId")
    @JsonAlias({"codigoCategoria", "siCodigo_Cat"})
    private Integer categoriaId;

    @Schema(description = "Código de la Subcategoría", example = "90")
    @JsonProperty("subCategoriaId")
    @JsonAlias({"codigoSubcategoria", "subcategoriaId", "siCodigo_SubCat"})
    private Integer subCategoriaId;

    @Schema(description = "Sumilla o resumen del requerimiento", example = "Problema con acceso a VPN")
    @JsonProperty("sumilla")
    @JsonAlias({"vSumilla_Req"})
    private String sumilla;

    @Schema(description = "Descripción detallada (HTML o texto)", example = "<p>No puedo conectarme...</p>")
    @JsonProperty("descripcionHtml")
    @JsonAlias({"descripcion", "vDescripcion_Req"})
    private String descripcionHtml;

    @Schema(description = "Código GR de la persona solicitante (opcional, si no se envía se usa el usuario autenticado)", example = "33")
    @JsonProperty("codigoPersonaSolicitante")
    @JsonAlias({"iCodigo_Per", "solicitanteId"})
    private Integer codigoPersonaSolicitante;

    @Schema(description = "Código de persona de actualización / auditoría", example = "1309")
    @JsonProperty("codigoPersonaActualizacion")
    @JsonAlias({"cCodPerActualizacion"})
    private String codigoPersonaActualizacion;

    @Schema(description = "Códigos de personal (cCodPer) de las personas en copia (CC). El SP espera cCodPer, no el código GR.")
    @JsonProperty("personasCopiaCodigos")
    private List<String> personasCopiaCodigos;

    @Schema(description = "Lista de IDs de personas para envío de copia (CC) - en desuso, usar personasCopiaCodigos")
    @JsonProperty("personasCopiaIds")
    private List<Integer> personasCopiaIds;

    @Schema(description = "Lista de datos complementarios dinámicos")
    @JsonProperty("datosComplementarios")
    private List<Map<String, Object>> datosComplementarios;

    @Schema(description = "Lista de archivos adjuntos")
    @JsonProperty("archivosAdjuntos")
    private List<Map<String, Object>> archivosAdjuntos;
}
