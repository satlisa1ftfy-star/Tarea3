package com.coresales.service.requerimiento.model;

import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import jakarta.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class AutorizarRequerimientoRequest extends BaseAuditoriaRequest {
        @Schema( description = "Código del requerimiento a asignar (iCodigo_Req).",
                example = "286768" )
        @Positive(message = "El código de requerimiento debe ser mayor a 0.")
        @NotNull( message = "El código de requerimiento es obligatorio.")
    private Integer codRequerimiento;
        @Schema(description = "Código de la persona que autoriza.",
                example = "286768")
        @Positive(message = "El código de Autorizacion debe ser mayor a 0.")
        @NotNull( message = "El código de Autorizacion es obligatorio.")
    private String codPerAutoriza;
    private String codPerSolAutorizacion;
    private String codPerSolAnulacion;
    private Integer codPerHelpDesk;
    private String observacion;
}
