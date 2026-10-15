package com.coresales.service.requerimiento.model;

import com.coresales.service.requerimiento.controller.BaseAuditoriaRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequerimientoResultado{
    private Integer codigoResultado;
    private String valorCad;
    private Integer valorInt;
    public RequerimientoResultado(Integer codigoResultado ) {
        this.codigoResultado = codigoResultado;
    }
}
