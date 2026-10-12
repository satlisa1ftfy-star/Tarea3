package com.coresales.service.requerimiento.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsignarRequerimientoRequest {

    @Schema(
            description = "Código del requerimiento a asignar (iCodigo_Req).",
            example = "286768"
    )
    private Integer codigoRequerimiento;

    @Schema(
            description = "Código GR de la persona que asigna, es decir el usuario " +
                    "logueado (iCodigo_Per). Viene del perfil ya autenticado.",
            example = "6073"
    )
    private Integer codigoPersonaAsigna;

    @Schema(
            description = "Código de persona del responsable " +
                    "destino, a quién se asigna (cCodPer). Es el mismo dato " +
                    "que 'codigoPersona' en GET /api/requerimiento/buscarNombre.",
            example = "0035"
    )
    private String codigoPersonaResponsable;

    @Schema(
            description = "Checkbox 'Responsable Principal' (bResPrincipal_ReqMov).",
            example = "false"
    )
    private Boolean responsablePrincipal;

    @Schema(
            description = "true = Informe Técnico, false = Descripción/Motivo " +
                    "(bInformeTecnico_ReqMov).",
            example = "false"
    )
    private Boolean informeTecnico;

    @Schema(
            description = "Texto del motivo o del informe técnico " +
                    "(vObservacion_ReqMov). Máximo 2000 caracteres " ,
            example = "Se asigna para atención de soporte técnico."
    )
    private String observacion;

    @Schema(
            description = "Código de persona de quien ejecuta " +
                    "la acción, para auditoría (cCodPerActualizacion). Es el propio " +
                    "'codigoPersona' del usuario logueado (el mismo dato que " +
                    "'codigoPersona' en GET /api/requerimiento/buscarNombre, pero de sí mismo).",
            example = "0027"
    )
    private String codigoPersonaActualizacion;
}
