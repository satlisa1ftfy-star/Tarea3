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
}
