package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolPersona {

    private Integer codigoRol;
    private String nombreRol;
    private String estado;
}