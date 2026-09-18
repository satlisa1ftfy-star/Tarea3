package com.coresales.service.user.auth.service;

import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import com.coresales.service.user.auth.repository.SeguridadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SeguridadServiceImpl
        implements ISeguridadService {

    private final SeguridadRepository seguridadRepository;

    public SeguridadServiceImpl(
            SeguridadRepository seguridadRepository
    ) {
        this.seguridadRepository =
                seguridadRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioSesion login(
            String usuarioWindows
    ) {

        String usuario = normalizar(usuarioWindows);

        UsuarioSesion usuarioSesion =
                seguridadRepository.consultarUsuario(usuario);

        if (usuarioSesion == null
                || usuarioSesion.getCodigoPersonaGr() == null
                || usuarioSesion.getCodigoPersonaGr() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "El usuario '" + usuario + "' no está registrado en Gestión de Requerimientos."
            );
        }

        if (usuarioSesion.getNumeroRoles() == null
                || usuarioSesion.getNumeroRoles() == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "El usuario '" + usuario + "' no tiene roles activos asignados."
            );
        }

        return usuarioSesion;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioRolDetalle> listarRoles(
            String usuarioWindows,
            Integer codigoRol
    ) {

        String usuario = normalizar(usuarioWindows);

        return seguridadRepository.listarRolesUsuario(
                usuario,
                codigoRol == null ? 0 : codigoRol
        );
    }

    private String normalizar(String usuarioWindows) {

        if (usuarioWindows == null || usuarioWindows.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el usuario de dominio (Windows)."
            );
        }

        return usuarioWindows.trim();
    }
}
