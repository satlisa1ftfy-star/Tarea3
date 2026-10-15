package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// spGR_Requerimiento_ConsultarPersonasAutorizan (bloque "Personas que Autorizan").
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonaAutoriza {

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaAutorizacion;
    private String codigoPersona;
    private String nombre;
    private String estado;
}
