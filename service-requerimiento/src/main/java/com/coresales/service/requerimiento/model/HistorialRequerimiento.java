package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// siTipBus = 1.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialRequerimiento {

    private Integer numero;
    private Integer codigoMovimiento;
    private LocalDateTime fecha;
    private String estado;
    private String personaOrigen;
    private String unidadOrigen;
    private String personaDestino;
    private String unidadDestino;
    private String observacion;
    private Boolean vigente;
}
