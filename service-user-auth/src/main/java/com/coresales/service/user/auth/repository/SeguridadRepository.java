package com.coresales.service.user.auth.repository;

import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;

import java.util.List;

public interface SeguridadRepository {

    UsuarioSesion consultarUsuario(
            String usuarioWindows
    );

    List<UsuarioRolDetalle> listarRolesUsuario(
            String usuarioWindows,
            Integer codigoRol
    );

    // Registra el ingreso con el rol elegido (GRMovAccesos)
    Integer registrarInicioSesion(
            Integer codigoPersonaGr,
            Integer codigoPersonaRol,
            String codigoSesion,
            String codigoPersonaActualizacion,
            String nombreTerminal
    );
}
