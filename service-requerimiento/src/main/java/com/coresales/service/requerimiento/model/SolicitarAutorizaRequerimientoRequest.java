package com.coresales.service.requerimiento.model;

import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class SolicitarAutorizaRequerimientoRequest extends BaseAuditoriaRequest {

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
        @Schema(description = "Código de la persona que solicita la autorizacion.",
                example = "286768")
        @Positive(message = "El código de la persona que solicita la Autorizacion debe ser mayor a 0.")
        @NotNull( message = "El código de la persona que solicita la Autorizacion es obligatorio.")
    private String codPerSolAutorizacion;
        @Schema(description = "Código de la persona que solicita la autorizacion.",
                example = "286768")
        @Positive(message = "El código de la persona que anula la solicitud de la Autorizacion debe ser mayor a 0.")
        @NotNull( message = "El código de la persona que anula la solicitud de la Autorizacion es obligatorio.")
    private String codPerSolAnulacion;
        @Schema(description = "Código de la persona de helpdesk.",
                example = "286768")
        @Positive(message = "El Código de la persona de helpdesk debe ser mayor a 0.")
        @NotNull( message = "El Código de la persona de helpdesk es obligatorio.")
    private Integer codPerHelpDesk;
        @Schema(description = "Observacion es obligatoria,
                example = "286768")
        @NotNull( message = "La observacion es obligatorio.")
    private String observacion;
}
