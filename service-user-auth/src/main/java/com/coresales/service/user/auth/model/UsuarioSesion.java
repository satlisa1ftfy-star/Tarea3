package com.coresales.service.user.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Confirma si un usuario de dominio (Windows) está registrado en
 * Gestión de Requerimientos y cuántos roles activos tiene asignados.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioSesion {

    private String usuarioWindows;
    private Integer numeroRoles;
    private Integer codigoPersonaGr;
    private String nombreCompleto;
    private String codigoPersonaOrganizacion;
}
