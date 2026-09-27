package com.coresales.service.user.auth.controller;

import com.coresales.service.user.auth.config.JwtTokenUtil;
import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import com.coresales.service.user.auth.service.ISeguridadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
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

    @PostMapping("/sesion")
    @Operation(
            summary = "Registrar inicio de sesión con el rol elegido",
            description = """
                    Requiere el token de /api/auth/login. Registra el ingreso en GRMovAccesos
                    (spGR_Seguridad_RegistrarInicioSesion).
                    """
    )
    public ResponseEntity<?> registrarSesion(
            @RequestBody Map<String, Object> request,
            Principal principal,
            HttpServletRequest httpRequest
    ) {

        Object valor = request.get("codigoPersonaRol");
        Integer codigoPersonaRol = null;

        if (valor instanceof Number numero) {
            codigoPersonaRol = numero.intValue();
        } else if (valor != null && !valor.toString().isBlank()) {
            try {
                codigoPersonaRol = Integer.valueOf(valor.toString().trim());
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "codigoPersonaRol debe ser numérico."
                );
            }
        }

        return ResponseEntity.ok(
                seguridadService.registrarInicioSesion(
                        principal.getName(),
                        codigoPersonaRol,
                        httpRequest.getRemoteAddr()
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
