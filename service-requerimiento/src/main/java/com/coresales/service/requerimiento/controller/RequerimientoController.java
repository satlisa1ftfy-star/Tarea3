package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.AsignarRequerimientoRequest;
import com.coresales.service.requerimiento.model.ClasificarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RegistrarRequerimientoRequest;
import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.model.RequerimientoAsignado;
import com.coresales.service.requerimiento.model.RequerimientoDetalleDTO;
import com.coresales.service.requerimiento.model.RequerimientoRegistroResponse;
import com.coresales.service.requerimiento.service.IRequerimientoAsignacionService;
import com.coresales.service.requerimiento.service.IRequerimientoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requerimiento")
@CrossOrigin({"http://localhost:5173", "http://localhost:5174"})
@Tag(
        name = "Gestión de Requerimientos",
        description = "Servicios de Gestión de Requerimientos"
)
public class RequerimientoController {

    private final IRequerimientoService requerimientoService;
    private final IRequerimientoAsignacionService requerimientoAsignacionService;

    public RequerimientoController(
            IRequerimientoService requerimientoService,
            IRequerimientoAsignacionService requerimientoAsignacionService
    ) {
        this.requerimientoService =
                requerimientoService;
        this.requerimientoAsignacionService =
                requerimientoAsignacionService;
    }

    @GetMapping("/buscar")
    @Operation(
            summary = "Buscar requerimientos",
            description = """
                    Búsqueda avanzada de requerimientos.

                    Valores que indican que el filtro no será considerado:

                    - numero = 0
                    - titulo = TODO
                    - fechaInicio = TODO
                    - fechaFin = TODO
                    - codigoPersonaSolicitante = 0
                    - codigoUoSolicitante = 0
                    - codigoPersonaResponsable = 0
                    - codigoUoResponsable = 0
                    - codigoEstado = 0
                    - vigencia = 3 (1 = vigente, 2 = no vigente, 3 = todos)

                    El formato de las fechas es yyyy-MM-dd.
                    """
    )
    public ResponseEntity<List<Requerimiento>> buscar(

            @Parameter(
                    description = "Número de requerimiento. 0 = todos.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer numero,

            @Parameter(
                    description = "Título o parte del título. TODO = todos.",
                    example = "TODO"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "TODO"
            )
            String titulo,

            @Parameter(
                    description = "Fecha inicial en formato yyyy-MM-dd. TODO = sin filtro.",
                    example = "TODO"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "TODO"
            )
            String fechaInicio,

            @Parameter(
                    description = "Fecha final en formato yyyy-MM-dd. TODO = sin filtro.",
                    example = "TODO"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "TODO"
            )
            String fechaFin,

            @Parameter(
                    description = "Código GR de la persona solicitante. 0 = todos.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoPersonaSolicitante,

            @Parameter(
                    description = "Código de unidad orgánica del solicitante. 0 = todas.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoUoSolicitante,

            @Parameter(
                    description = "Código GR de la persona responsable. 0 = todos.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoPersonaResponsable,

            @Parameter(
                    description = "Código de unidad orgánica del responsable. 0 = todas.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoUoResponsable,

            @Parameter(
                    description = "Código del estado. 0 = todos.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoEstado,

            @Parameter(
                    description = "Vigencia del requerimiento. 1 = vigente, 2 = no vigente, 3 = todos.",
                    example = "3"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "3"
            )
            Integer vigencia
    ) {

        return ResponseEntity.ok(
                requerimientoService.buscar(
                        numero,
                        titulo,
                        fechaInicio,
                        fechaFin,
                        codigoPersonaSolicitante,
                        codigoUoSolicitante,
                        codigoPersonaResponsable,
                        codigoUoResponsable,
                        codigoEstado,
                        vigencia
                )
        );
    }

    @PostMapping("/asignar")
    @Operation(
            summary = "Asignar requerimiento",
            description = """
                    Asigna un requerimiento a una persona responsable
                    """
    )
    public ResponseEntity<RequerimientoAsignado> asignar(
            @RequestBody
            AsignarRequerimientoRequest solicitud,
            HttpServletRequest httpRequest
    ) {

        String ipCliente =
                obtenerIpCliente(httpRequest);

        return ResponseEntity.ok(
                requerimientoAsignacionService.asignar(
                        solicitud,
                        ipCliente
                )
        );
    }

    @PostMapping("/registrar")
    @Operation(
            summary = "Registrar requerimiento",
            description = "Registra un nuevo requerimiento mediante spGR_Requerimiento_Registrar (siTipBus = 1)."
    )
    public ResponseEntity<RequerimientoRegistroResponse> registrar(
            @RequestBody
            RegistrarRequerimientoRequest solicitud,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(
                requerimientoService.registrar(solicitud, obtenerIpCliente(httpRequest))
        );
    }

    @GetMapping("/{id}/para-clasificar")
    @Operation(
            summary = "Obtener requerimiento para clasificar",
            description = "Detalle completo del requerimiento mediante spGR_Requerimiento_Consultar (siTipBus = 2 y 3)."
    )
    public ResponseEntity<RequerimientoDetalleDTO> obtenerParaClasificar(
            @PathVariable Integer id
    ) {
        RequerimientoDetalleDTO detalle = requerimientoService.obtenerParaClasificar(id);

        if (detalle == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detalle);
    }

    @PutMapping("/{id}/clasificacion")
    @Operation(
            summary = "Clasificar requerimiento",
            description = "Actualiza la clasificación técnica mediante spGR_Requerimiento_Registrar (siTipBus = 2)."
    )
    public ResponseEntity<Void> clasificar(
            @PathVariable Integer id,
            @RequestBody
            ClasificarRequerimientoRequest solicitud,
            HttpServletRequest httpRequest
    ) {
        solicitud.setRequerimientoId(id);
        requerimientoService.clasificar(solicitud, obtenerIpCliente(httpRequest));
        return ResponseEntity.ok().build();
    }

    /**
     * Lee la IP real del usuario desde 'X-Forwarded-For' por si la petición pasa
     * por un proxy/balanceador. Si no existe la cabecera, usa la IP directa (getRemoteAddr).
     */
    private String obtenerIpCliente(HttpServletRequest request) {

        String forwardedFor =
                request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}