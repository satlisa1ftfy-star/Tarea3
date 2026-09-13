package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.service.IRequerimientoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
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

    public RequerimientoController(
            IRequerimientoService requerimientoService
    ) {
        this.requerimientoService =
                requerimientoService;
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
}