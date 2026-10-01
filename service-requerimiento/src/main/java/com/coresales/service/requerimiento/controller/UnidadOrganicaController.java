package com.coresales.service.requerimiento.controller;

import com.coresales.service.requerimiento.model.UnidadOrganica;
import com.coresales.service.requerimiento.service.IUnidadOrganicaService;
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

    @GetMapping("/unidadesOrganicas/solicitud")
    @Operation(
            summary = "Unidades orgánicas a las que se puede solicitar",
            description = "Todas las unidades orgánicas para registrar un requerimiento "
                    + "(spGR_UnidadOrganica_Consultar siTipBus=7). El código devuelto es el iCodUO."
    )
    public ResponseEntity<List<UnidadOrganica>> paraSolicitud() {
        return ResponseEntity.ok(unidadOrganicaService.listarParaSolicitud());
    }

    @GetMapping("/unidadesOrganicas/{codigoUo}/dependencias")
    @Operation(
            summary = "Dependencias de una unidad orgánica",
            description = "Divisiones/unidades hijas (spGR_UnidadOrganica_Consultar siTipBus=2). "
                    + "codigoUo es el iCodUO de la unidad padre."
    )
    public ResponseEntity<List<UnidadOrganica>> dependencias(@PathVariable Integer codigoUo) {
        return ResponseEntity.ok(unidadOrganicaService.buscarDependencias(codigoUo));
    }
}
