package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.RolPersona;
import com.coresales.service.requerimiento.service.IRolPersonaService;

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
public class RolPersonaController {

    private final IRolPersonaService rolPersonaService;

    public RolPersonaController(
            IRolPersonaService rolPersonaService
    ) {
        this.rolPersonaService =
                rolPersonaService;
    }

    @GetMapping("/consultarRolPersona/{codigoPer}")
    @Operation(
            summary = "Consultar roles por persona",
            description = "Lista todos los roles activos asignados a una persona de Gestión de Requerimientos."
    )
    public ResponseEntity<List<RolPersona>> consultar(

            @Parameter(
                    description = "Código de persona de Gestión de Requerimientos",
                    example = "115"
            )
            @PathVariable
            Integer codigoPer
    ) {

        return ResponseEntity.ok(
                rolPersonaService.listarPorPersona(
                        codigoPer
                )
        );
    }
}
