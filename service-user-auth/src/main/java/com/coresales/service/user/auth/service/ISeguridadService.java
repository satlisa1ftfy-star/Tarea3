package com.coresales.service.user.auth.service;

import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;

import java.util.List;

public interface ISeguridadService {

    UsuarioSesion login(
            String usuarioWindows
    );

    List<UsuarioRolDetalle> listarRoles(
            String usuarioWindows,
            Integer codigoRol
    );
}
