package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.Estado;
import com.coresales.service.requerimiento.service.IEstadoService;

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
public class EstadoController {

    private final IEstadoService estadoService;

    public EstadoController(
            IEstadoService estadoService
    ) {
        this.estadoService =
                estadoService;
    }

    @GetMapping("/buscarEstado/{nomEst}")
    @Operation(
            summary = "Buscar estado",
            description = "Busca estados de requerimiento por nombre. TODO lista todos los estados activos."
    )
    public ResponseEntity<List<Estado>> buscar(

            @Parameter(
                    description = "Nombre o parte del nombre del estado. TODO = todos.",
                    example = "CERRADO"
            )
            @PathVariable
            String nomEst
    ) {

        return ResponseEntity.ok(
                estadoService.buscar(
                        nomEst
                )
        );
    }
}
