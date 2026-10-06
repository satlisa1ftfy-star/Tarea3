package com.coresales.service.user.auth.service;

import com.coresales.service.user.auth.model.SesionResponse;
import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import com.coresales.service.user.auth.repository.SeguridadRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

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

    /**
     * Registra el ingreso con el rol elegido (spGR_Seguridad_RegistrarInicioSesion).
     * El rol debe pertenecer al usuario del token; persona y cCodPer se toman del propio rol.
     */
    @Override
    @Transactional
    public SesionResponse registrarInicioSesion(
            String usuarioWindows,
            Integer codigoPersonaRol,
            String nombreTerminal
    ) {

        String usuario = normalizar(usuarioWindows);

        if (codigoPersonaRol == null || codigoPersonaRol <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el codigoPersonaRol del rol elegido."
            );
        }

        UsuarioRolDetalle rol =
                seguridadRepository.listarRolesUsuario(usuario, 0)
                        .stream()
                        .filter(r -> codigoPersonaRol.equals(r.getCodigoPersonaRol()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "El rol indicado no pertenece al usuario '" + usuario + "' o no está activo."
                        ));

        // vCodigoSesion_Ac es Varchar(30)
        String codigoSesion =
                UUID.randomUUID().toString().replace("-", "").substring(0, 30);

        Integer codigoIngreso =
                seguridadRepository.registrarInicioSesion(
                        rol.getCodigoPersonaGr(),
                        rol.getCodigoPersonaRol(),
                        codigoSesion,
                        rol.getCodigoPersonaOrganizacion(),
                        recortar(nombreTerminal, 20)   // cNombreTerminal_Ac es Char(20)
                );

        return new SesionResponse(
                codigoIngreso,
                codigoSesion,
                rol.getCodigoPersonaGr(),
                rol.getCodigoPersonaRol(),
                rol.getCodigoRol(),
                rol.getNombreRol()
        );
    }

    private String recortar(String valor, int maximo) {

        if (valor == null) {
            return "";
        }

        String limpio = valor.trim();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
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
