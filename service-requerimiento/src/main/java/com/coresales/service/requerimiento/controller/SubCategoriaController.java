package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.SubCategoria;
import com.coresales.service.requerimiento.service.ISubCategoriaService;

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
public class SubCategoriaController {

    private final ISubCategoriaService subCategoriaService;

    public SubCategoriaController(
            ISubCategoriaService subCategoriaService
    ) {
        this.subCategoriaService =
                subCategoriaService;
    }

    @GetMapping("/buscarSubCategoria/{codCat}/{nomSubCat}")
    @Operation(
            summary = "Buscar subcategoría",
            description = "Busca subcategorías pertenecientes a una categoría según el término ingresado."
    )
    public ResponseEntity<List<SubCategoria>> buscar(

            @Parameter(
                    description = "Código de la categoría. 0 = todas.",
                    example = "3"
            )
            @PathVariable
            Integer codCat,

            @Parameter(
                    description = "Nombre o parte del nombre de la subcategoría. TODO = todas.",
                    example = "TODO"
            )
            @PathVariable
            String nomSubCat
    ) {

        return ResponseEntity.ok(
                subCategoriaService.buscar(
                        codCat,
                        nomSubCat
                )
        );
    }
}
