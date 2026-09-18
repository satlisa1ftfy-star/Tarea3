package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    private Integer codigoPer;
    private String unidadOrganica;
    private String nombre;
    private String observacion;
    private String vigencia;
    private Integer codigoUo;
    private String codigoPersona;
    private Boolean vigente;

    // Nuevos
    private Integer codigoCargo;
    private String cargo;
}