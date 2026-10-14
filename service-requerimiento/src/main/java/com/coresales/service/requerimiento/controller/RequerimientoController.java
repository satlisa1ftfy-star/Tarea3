package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.dto.response.ConsultaSolicitudAutorizaResponse;
import com.coresales.service.requerimiento.model.AsignarRequerimientoRequest;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.DocumentoAdjuntoDetalle;
import com.coresales.service.requerimiento.model.HistorialRequerimiento;
import com.coresales.service.requerimiento.model.PersonaAutoriza;
import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.model.RequerimientoAsignado;
import com.coresales.service.requerimiento.model.RequerimientoDetalle;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;
import com.coresales.service.requerimiento.service.IRequerimientoAsignacionService;
import com.coresales.service.requerimiento.service.IRequerimientoAutorizarService;
import com.coresales.service.requerimiento.service.IRequerimientoDetalleService;
import com.coresales.service.requerimiento.service.IRequerimientoService;
import com.coresales.service.requerimiento.service.IRequerimientoSolicitaAutorizaService;

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
@CrossOrigin("http://localhost:5173")
@Tag(
        name = "Gestión de Requerimientos",
        description = "Servicios de Gestión de Requerimientos"
)
public class RequerimientoController {

    private final IRequerimientoService requerimientoService;
    private final IRequerimientoAsignacionService requerimientoAsignacionService;
    private final IRequerimientoDetalleService requerimientoDetalleService;
    private final IRequerimientoAutorizarService requerimientoAutorizarService;
    private final IRequerimientoSolicitaAutorizaService requerimientoSolicitaAutorizaService;

    public RequerimientoController(
            IRequerimientoService requerimientoService,
            IRequerimientoAsignacionService requerimientoAsignacionService,
            IRequerimientoDetalleService requerimientoDetalleService,
            IRequerimientoAutorizarService requerimientoAutorizarService,
            IRequerimientoSolicitaAutorizaService requerimientoSolicitaAutorizaService
    ) {
        this.requerimientoService =
                requerimientoService;
        this.requerimientoAsignacionService =
                requerimientoAsignacionService;
        this.requerimientoDetalleService =
                requerimientoDetalleService;
        this.requerimientoAutorizarService =
                requerimientoAutorizarService;
        this.requerimientoSolicitaAutorizaService =
                requerimientoSolicitaAutorizaService;
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
                    - codigoUoRequerimiento = 0 (UO de la ubicación actual)
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
                    description = "Código de unidad orgánica de la ubicación actual del requerimiento. 0 = todas.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoUoRequerimiento,

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
                        codigoUoRequerimiento,
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

    @GetMapping("/detalle/{codigo}")
    @Operation(
            summary = "Detalle de requerimiento",
            description = """
                    Datos de la pantalla "Detalle de Requerimiento" del legado
                    incluye medio de envío, ubicación actual y correos copia.
                    """
    )
    public ResponseEntity<RequerimientoDetalle> detalle(
            @Parameter(description = "Número de requerimiento.", example = "280177")
            @PathVariable
            Integer codigo
    ) {
        return ResponseEntity.ok(
                requerimientoDetalleService.consultarDetalle(codigo)
        );
    }

    @GetMapping("/historial/{codigo}")
    @Operation(
            summary = "Historial de requerimiento",
            description = """
                    Movimientos del requerimiento, del más reciente al más antiguo
                    (spGR_Requerimiento_ConsultarHistorial, siTipBus = 1).
                    """
    )
    public ResponseEntity<List<HistorialRequerimiento>> historial(
            @Parameter(description = "Número de requerimiento.", example = "280177")
            @PathVariable
            Integer codigo
    ) {
        return ResponseEntity.ok(
                requerimientoDetalleService.consultarHistorial(codigo)
        );
    }

    @GetMapping("/adjuntos/{codigo}")
    @Operation(
            summary = "Documentos adjuntos del requerimiento",
            description = """
                    Archivo cargado en el registro y adjuntos posteriores
                    (spGR_Requerimiento_ConsultarDocumentoAdjunto, siTipBus = 1). Solo lista; la descarga
                    Archivo adjuntos pendiente del servicio de documentos.
                    """
    )
    public ResponseEntity<List<DocumentoAdjuntoDetalle>> adjuntos(
            @Parameter(description = "Número de requerimiento.", example = "247676")
            @PathVariable
            Integer codigo
    ) {
        return ResponseEntity.ok(
                requerimientoDetalleService.consultarAdjuntos(codigo)
        );
    }

    @GetMapping("/autorizaciones/{codigo}")
    @Operation(
            summary = "Personas que autorizan",
            description = """
                    Bloque "Personas que Autorizan" del detalle: fechas de solicitud y autorización,
                    código, nombre y estado (spGR_Requerimiento_ConsultarPersonasAutorizan).
                    """
    )
    public ResponseEntity<List<PersonaAutoriza>> autorizaciones(
            @Parameter(description = "Número de requerimiento.", example = "286933")
            @PathVariable
            Integer codigo
    ) {
        return ResponseEntity.ok(
                requerimientoDetalleService.consultarPersonasAutorizan(codigo)
        );
    }

    // Autorizaciones (Jhon): spGR_Requerimiento_RegistrarPersonaAutorizacion
    // y spGR_Requerimiento_ConsultarPersonaAutorizacion.
    @PostMapping("/solicitar")
    @Operation(
            summary = "Solicitar autorización",
            description = "Registra a las personas que deben autorizar el requerimiento (siTipBus = 1)."
    )
    public ResponseEntity<RequerimientoResultado> solicitar(
            @RequestBody
            SolicitarAutorizaRequerimientoRequest solicitud,
            HttpServletRequest httpRequest
    ) {
        solicitud.setNomTerminal(obtenerIpCliente(httpRequest));

        return ResponseEntity.ok(
                requerimientoSolicitaAutorizaService.solicitarAutorizacion(solicitud)
        );
    }

    @PostMapping("/autorizar")
    @Operation(
            summary = "Autorizar requerimiento",
            description = "Autoriza o no autoriza la atención del requerimiento (siTipBus = 2)."
    )
    public ResponseEntity<RequerimientoResultado> autorizar(
            @RequestBody
            AutorizarRequerimientoRequest solicitud,
            HttpServletRequest httpRequest
    ) {
        solicitud.setNomTerminal(obtenerIpCliente(httpRequest));

        return ResponseEntity.ok(
                requerimientoAutorizarService.autorizar(solicitud)
        );
    }

    @GetMapping("/consultarSolicitud")
    @Operation(
            summary = "Consultar personas que autorizan",
            description = "Personas a las que se solicitó autorización y su estado (siTipBus = 1)."
    )
    public ResponseEntity<List<ConsultaSolicitudAutorizaResponse>> consultarSolicitud(
            @Parameter(description = "Número de requerimiento.", example = "280177")
            @RequestParam
            Integer codigoRequerimiento
    ) {
        return ResponseEntity.ok(
                requerimientoSolicitaAutorizaService.consultarSolicitud("1", codigoRequerimiento)
        );
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