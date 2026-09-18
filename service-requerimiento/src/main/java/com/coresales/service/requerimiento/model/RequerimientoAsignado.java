package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequerimientoAsignado {

    private Integer codigoRequerimiento;
    private String nombreTerminal;
}
