package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Categoría con el tipo de activo al que pertenece (lo necesita el registro de requerimientos).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaActivo {

    private Integer codigoCategoria;
    private String nombreCategoria;
    private Integer codigoActivo;
    private String nombreActivo;
}
