package com.coresales.service.requerimiento.model;

import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(callSuper = true)
public class SolicitarAutorizaRequerimientoRequest extends BaseAuditoriaRequest {

        @Schema( description = "Código del requerimiento (iCodigo_Req).",
                example = "286768" )
        @Positive(message = "El código de requerimiento debe ser mayor a 0.")
        @NotNull( message = "El código de requerimiento es obligatorio.")
    private Integer codRequerimiento;
        @Schema(description = "cCodPer de las personas que autorizan, separados por coma (vCodPerAutorizacion).",
                example = "0307,0524")
        @NotBlank( message = "Las personas que autorizan son obligatorias.")
    private String codPerAutoriza;
        @Schema(description = "Código GR (iCodigo_Per) de quien solicita la autorización.",
                example = "6073")
        @NotNull( message = "El código de la persona que solicita la autorización es obligatorio.")
    private String codPerSolAutorizacion;
        @Schema(description = "Código GR de quien anula la solicitud (solo anulación).",
                example = "6073")
    private String codPerSolAnulacion;
        @Schema(description = "Código GR (iCodigo_Per) del usuario de helpdesk que registra.",
                example = "6073")
        @Positive(message = "El código de la persona de helpdesk debe ser mayor a 0.")
        @NotNull( message = "El código de la persona de helpdesk es obligatorio.")
    private Integer codPerHelpDesk;
        @Schema(description = "Motivo de la solicitud (máx. 2000).",
                example = "Se solicita autorización para la atención")
        @NotBlank( message = "La observación es obligatoria.")
    private String observacion;
}
