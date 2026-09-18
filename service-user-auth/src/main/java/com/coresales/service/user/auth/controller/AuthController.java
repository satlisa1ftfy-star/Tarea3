package com.coresales.service.user.auth.controller;

import com.coresales.service.user.auth.config.JwtTokenUtil;
import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import com.coresales.service.user.auth.service.ISeguridadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * solo existe el usuario de dominio (Windows), ya autenticado por el sistema operativo antes de llegar acá.
 * Por eso "login" aquí no valida contraseña
 */
@RestController
@RequestMapping("/api/auth")
@Tag(
        name = "Autenticación",
        description = "Inicio de sesión (usuario de dominio/Windows) y emisión de JWT para Gestión de Requerimientos"
)
public class AuthController {

    private final ISeguridadService seguridadService;
    private final JwtTokenUtil jwtTokenUtil;

    public AuthController(
            ISeguridadService seguridadService,
            JwtTokenUtil jwtTokenUtil
    ) {
        this.seguridadService = seguridadService;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = """
                    solo usuario de dominio/Windows). Responde 401 si el usuario no está registrado y 403
                    si no tiene roles activos asignados.
                    """
    )
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request
    ) {

        String usuarioWindows = request.get("usuarioWindows");

        // seguridadService.login lanza ResponseStatusException (401/403)
        // si el usuario no existe o no tiene roles; Spring la traduce
        // automáticamente en la respuesta HTTP correspondiente.
        UsuarioSesion usuarioSesion =
                seguridadService.login(usuarioWindows);

        String token =
                jwtTokenUtil.generateToken(usuarioSesion.getUsuarioWindows());

        return ResponseEntity.ok(
                Map.of(
                        "token", token,
                        "expiresAt", jwtTokenUtil.getExpirationInstant(token).toString(),
                        "perfil", usuarioSesion
                )
        );
    }

    @GetMapping("/roles/{usuarioWindows}")
    @Operation(
            summary = "Listar roles del usuario",
            description = """
                    Lista los roles activos del usuario de dominio (Windows)
                    codigoRol = 0 lista todos los roles.
                    """
    )
    public ResponseEntity<List<UsuarioRolDetalle>> roles(

            @Parameter(
                    description = "Usuario de dominio (Windows)",
                    example = "JGUILLEN"
            )
            @PathVariable
            String usuarioWindows,

            @Parameter(
                    description = "Código de rol. 0 = todos.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoRol
    ) {

        return ResponseEntity.ok(
                seguridadService.listarRoles(
                        usuarioWindows,
                        codigoRol
                )
        );
    }
}
