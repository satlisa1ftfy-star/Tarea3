package com.coresales.service.product.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Requerimiento {

    private Integer codigoRequerimiento;
    private String titulo;
    private LocalDateTime fechaRegistro;
    private String descripcion;

    // Solicitante
    private Integer codigoPersonaSolicitante;
    private String solicitante;
    private Integer codigoUoSolicitante;
    private String unidadOrganicaSolicitante;
    private Integer codigoCargoSolicitante;
    private String cargoSolicitante;

    // Responsable
    private Integer codigoPersonaResponsable;
    private String responsable;
    private Integer codigoUoResponsable;
    private String unidadOrganicaResponsable;
    private Integer codigoCargoResponsable;
    private String cargoResponsable;

    // Estado
    private Integer codigoEstado;
    private String estado;

    // Categoría
    private Integer codigoCategoria;
    private String categoria;

    // Subcategoría
    private Integer codigoSubcategoria;
    private String subcategoria;

    // Prioridad
    private Integer codigoPrioridad;
    private String prioridad;

    // Unidad orgánica a la que pertenece el requerimiento
    private Integer codigoUoRequerimiento;
    private String unidadOrganicaRequerimiento;

    // Relación categoría-subcategoría
    private Integer codigoCategoriaSubcategoria;
}