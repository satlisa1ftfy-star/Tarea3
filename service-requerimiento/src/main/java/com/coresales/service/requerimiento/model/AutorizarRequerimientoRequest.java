package com.coresales.service.requerimiento.model;

import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.*;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class AutorizarRequerimientoRequest extends BaseAuditoriaRequest {
        @Schema( description = "Código del requerimiento (iCodigo_Req).",
                example = "286768" )
        @Positive(message = "El código de requerimiento debe ser mayor a 0.")
        @NotNull( message = "El código de requerimiento es obligatorio.")
    private Integer codRequerimiento;
        @Schema(description = "No lo usa el SP en siTipBus = 2 (se conserva por compatibilidad).",
                example = "0307")
    private String codPerAutoriza;
    private String codPerSolAutorizacion;
    private String codPerSolAnulacion;
        @Schema(description = "Código GR (iCodigo_Per) de la persona que autoriza (usuario logueado).",
                example = "6073")
        @Positive(message = "El código de la persona que autoriza debe ser mayor a 0.")
        @NotNull( message = "El código de la persona que autoriza es obligatorio.")
    private Integer codPerHelpDesk;
        @Schema(description = "true = Autorizado (10), false = No autorizado (11). Por defecto true.",
                example = "true")
    private Boolean autoriza;
    private String observacion;
}
