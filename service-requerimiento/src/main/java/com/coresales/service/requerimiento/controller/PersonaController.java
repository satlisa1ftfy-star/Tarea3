package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.Persona;
import com.coresales.service.requerimiento.service.IPersonaService;
import org.springframework.http.ResponseEntity;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;

import java.util.List;

@RestController
@CrossOrigin({"http://localhost:5173", "http://localhost:5174"})
@Tag(
        name = "Gestión de Requerimientos",
        description = "Servicios de Gestión de Requerimientos"
)
public class PersonaController {

    private final IPersonaService personaService;

    public PersonaController(
            IPersonaService personaService
    ) {
        this.personaService =
                personaService;
    }

    @GetMapping({"/api/requerimiento/buscarNombre", "/api/personas"})
    @Operation(
            summary = "Buscar personas",
            description = """
                    Busca personas GR (GRMaePersona, BD GestionRQLD) por nombre,
                    unidad orgánica y vigencia. SP: spGR_Persona_ConsultarNombreUO.

                    Vigencia:
                    1 = Vigente
                    2 = No Vigente
                    3 = Todos
                    """
    )
    public ResponseEntity<List<Persona>> listar(

            @Parameter(
                    description = "Nombre o parte del nombre. TODO = todos.",
                    example = "TODO"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "TODO"
            )
            String nombre,

            @Parameter(
                    description = "Código de unidad orgánica. 0 = todas.",
                    example = "0"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "0"
            )
            Integer codigoUo,

            @Parameter(
                    description = "1 = Vigente, 2 = No Vigente, 3 = Todos.",
                    example = "3"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "3"
            )
            Integer vigencia
    ) {

        return ResponseEntity.ok(
                personaService.listar(
                        nombre,
                        codigoUo,
                        vigencia
                )
        );
    }
}