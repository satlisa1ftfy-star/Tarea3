package com.coresales.service.requerimiento.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsultaSolicitudAutorizaResponse {
    private String codigo;
    private String nombre;
    private String estado;
}