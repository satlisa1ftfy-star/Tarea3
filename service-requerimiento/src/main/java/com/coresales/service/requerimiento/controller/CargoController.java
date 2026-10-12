package com.coresales.service.requerimiento.controller;


import com.coresales.service.requerimiento.dto.response.CargoResponse;
import com.coresales.service.requerimiento.model.Categoria;
import com.coresales.service.requerimiento.service.ICargoService;
import com.coresales.service.requerimiento.service.ICategoriaService;
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
public class CargoController {

    private final ICargoService cargoService;

    public CargoController(
            ICargoService cargoService
    ) {
        this.cargoService =
                cargoService;
    }

    @GetMapping("/buscarCargo")
    @Operation(
            summary = "Buscar cargo",
            description = "Busca cargo por nombre y unidad orgánica. TODO y 0 permiten listar todas."
    )
    public ResponseEntity<List<CargoResponse>> buscar(
            @RequestParam(required = false,defaultValue = "1")
            Integer TipBus,
            @RequestParam(required = false,defaultValue = "0")
            Integer codigoReq,
            @RequestParam(required = false,defaultValue = "0")
            Integer codigoCat,
            @RequestParam(required = false,defaultValue = "0")
            Integer Codigo_SubCat
    ) {

        return ResponseEntity.ok(
                cargoService.buscar(
                        TipBus,
                        codigoReq,
                        codigoCat,
                        Codigo_SubCat
                )
        );
    }
}