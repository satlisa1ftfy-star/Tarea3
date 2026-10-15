package com.coresales.service.requerimiento.controller;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public abstract class BaseAuditoriaRequest {

    @Schema(
            description = "Código de persona de quien ejecuta " +
                    "la acción, para auditoría (cCodPerActualizacion). Es el propio " +
                    "'codigoPersona' del usuario logueado (el mismo dato que " +
                    "'codigoPersona' en GET /api/personas, pero de sí mismo).",
            example = "0027"
    )
    @NotBlank(message = "El código de persona de auditoría es obligatorio.")
    private String codPerActualiza;

    @Schema(
            description = "Nombre del terminal donde se ejecuta la peticion " ,
            example = "P7a-aso30"
    )
    @NotBlank(message = "El Terminal o el IP de la paersona que ingresa al sistema")
    private String nomTerminal;
}