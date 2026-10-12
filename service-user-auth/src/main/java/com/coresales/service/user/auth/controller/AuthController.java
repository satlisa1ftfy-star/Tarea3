package com.coresales.service.user.auth.controller;

import com.coresales.service.user.auth.config.JwtTokenUtil;
import com.coresales.service.user.auth.config.OpenApiConfig;
import com.coresales.service.user.auth.model.LoginRequest;
import com.coresales.service.user.auth.model.LoginResponse;
import com.coresales.service.user.auth.model.SesionRequest;
import com.coresales.service.user.auth.model.SesionResponse;
import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import com.coresales.service.user.auth.service.ISeguridadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * solo existe el usuario de dominio (Windows), ya autenticado por el sistema operativo antes de llegar acá.
 * Por eso "login" aquí no valida contraseña
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Iniciar sesión",
            description = """
                    Solo usuario de dominio/Windows
                    Errores: 400 si no envía el usuario, 401 si no está registrado, 403 si no tiene roles activos.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Token emitido y perfil del usuario")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request
    ) {

        // seguridadService.login lanza ResponseStatusException (400/401/403)
        // si falta el usuario, no existe o no tiene roles; Spring la traduce
        // automáticamente en la respuesta HTTP correspondiente.
        UsuarioSesion usuarioSesion =
                seguridadService.login(request.getUsuarioWindows());

        String token =
                jwtTokenUtil.generateToken(usuarioSesion.getUsuarioWindows());

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        jwtTokenUtil.getExpirationInstant(token).toString(),
                        usuarioSesion
                )
        );
    }

    @PostMapping(value = "/sesion", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Registrar inicio de sesión con el rol elegido",
            description = """
                    Registra el ingreso en GRMovAccesos.
                    El codigoPersonaRol debe ser uno de SUS roles (Login)

                    Errores: 400 si falta o no es numérico, 401 sin token o token vencido, 403 si el rol no es suyo o no
                     está activo.
                    """,
            security = @SecurityRequirement(name = OpenApiConfig.SCHEME_NAME)
    )
    @ApiResponse(responseCode = "200", description = "Ingreso registrado")
    public ResponseEntity<SesionResponse> registrarSesion(
            @RequestBody SesionRequest request,
            @Parameter(hidden = true) Principal principal,
            @Parameter(hidden = true) HttpServletRequest httpRequest
    ) {

        return ResponseEntity.ok(
                seguridadService.registrarInicioSesion(
                        principal.getName(),
                        request.getCodigoPersonaRol(),
                        httpRequest.getRemoteAddr()
                )
        );
    }

    /**
     * Body vacío, JSON mal formado o codigoPersonaRol no numérico -> 400 con mensaje claro
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> bodyInvalido(
            HttpMessageNotReadableException e,
            HttpServletRequest httpRequest
    ) {
        // Mismo formato que el resto de errores (timestamp, status, error, message, path)
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("timestamp", Instant.now().toString());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        error.put("message", "Body inválido: debe ser un JSON válido con los tipos correctos (por ejemplo, " +
                "codigoPersonaRol numérico).");
        error.put("path", httpRequest.getRequestURI());
        return ResponseEntity.badRequest().body(error);
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
