package com.coresales.service.organizacion.controller;

import com.coresales.service.organizacion.model.UnidadOrganica;
import com.coresales.service.organizacion.service.IUnidadOrganicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizacion")
@CrossOrigin("http://localhost:5173")
@Tag(
        name = "Organización",
        description = "Consulta de unidades orgánicas y personal (BD Organizacion)"
)
public class UnidadOrganicaController {

    private final IUnidadOrganicaService unidadOrganicaService;

    public UnidadOrganicaController(
            IUnidadOrganicaService unidadOrganicaService
    ) {
        this.unidadOrganicaService =
                unidadOrganicaService;
    }

    @GetMapping("/buscarUnidadOrganica/{nomUO}")
    @Operation(
            summary = "Buscar unidad orgánica",
            description = "Busca unidades orgánicas cuyo nombre coincida con el término ingresado."
    )
    public ResponseEntity<List<UnidadOrganica>> buscar(

            @Parameter(
                    description = "Nombre o parte del nombre de la unidad orgánica",
                    example = "informatica"
            )
            @PathVariable
            String nomUO
    ) {

        return ResponseEntity.ok(
                unidadOrganicaService.buscar(
                        nomUO
                )
        );
    }
}