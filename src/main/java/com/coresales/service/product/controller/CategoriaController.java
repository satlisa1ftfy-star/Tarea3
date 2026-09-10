package com.coresales.service.product.controller;


import com.coresales.service.product.model.Categoria;
import com.coresales.service.product.service.ICategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/requerimiento")
@Tag(
        name = "Gestión de Requerimientos",
        description = "Servicios de Gestión de Requerimientos"
)
public class CategoriaController {

    private final ICategoriaService categoriaService;

    public CategoriaController(
            ICategoriaService categoriaService
    ) {
        this.categoriaService =
                categoriaService;
    }

    @GetMapping("/buscarCategoria")
    @Operation(
            summary = "Buscar categoría",
            description = "Busca categorías por nombre y unidad orgánica. TODO y 0 permiten listar todas."
    )
    public ResponseEntity<List<Categoria>> buscar(

            @Parameter(
                    description = "Nombre o parte del nombre. TODO = todas.",
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
            Integer codigoUo
    ) {

        return ResponseEntity.ok(
                categoriaService.buscar(
                        nombre,
                        codigoUo
                )
        );
    }
}