package com.coresales.service.requerimiento.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequerimientoRegistroResponse {

    @JsonProperty("requerimientoId")
    private Integer requerimientoId;

    @JsonProperty("numeroRequerimiento")
    private String numeroRequerimiento;

    @JsonProperty("fechaRegistro")
    private String fechaRegistro;
}
