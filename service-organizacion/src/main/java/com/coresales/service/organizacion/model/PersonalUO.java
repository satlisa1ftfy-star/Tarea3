package com.coresales.service.organizacion.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Personal de una unidad orgánica (BD Organizacion).
 * Origen: organizacion.dbo.spEO_Personal_BuscarPersonalxUO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonalUO {

    private String codigoPersonal;      // CCODPER
    private String usuarioWindows;      // VUSUWIN
    private String dni;                 // VNUMDNI
    private String nombre;              // VNOMBRE (APEPAT APEMAT, NOMBRE)
    private Integer codigoUo;           // NCODUO
    private String unidadOrganica;      // VDESLUO
    private Integer codigoCargo;        // NCODCAR
    private String cargo;               // VDESCAR
    private String estado;              // VNOMEST
    private String categoria;           // VNOMCAT
    private String correo;              // VCORREO
    private String ubicacion;           // VNOMUBI
    private Integer cantidadUoJefe;     // CCODJEF (n.º de UO donde es jefe)
    private Boolean jefe;               // CCODJEF > 0
}
