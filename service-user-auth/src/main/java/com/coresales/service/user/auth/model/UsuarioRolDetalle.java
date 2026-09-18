package com.coresales.service.user.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRolDetalle {

    private Integer codigoPersonaGr;
    private String codigoPersonaOrganizacion;
    private String usuarioWindows;
    private String numeroDocumento;
    private String nombres;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private Integer codigoUnidadOrganica;
    private String unidadOrganica;
    private Integer codigoCargo;
    private String cargo;
    private String estadoPersonal;
    private String categoria;
    private String correo;

    private Integer codigoRol;
    private String nombreRol;
    private Integer codigoPersonaRol;
    private Integer codigoTipoRol;
    private String nombreTipoRol;
}
