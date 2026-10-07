package com.coresales.service.organizacion.controller;

import com.coresales.service.organizacion.model.PersonalUO;
import com.coresales.service.organizacion.service.IPersonalUOService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizacion")
@CrossOrigin({"http://localhost:5173", "http://localhost:5174"})
@Tag(
        name = "Organización",
        description = "Consulta de unidades orgánicas y personal (BD Organizacion)"
)
public class PersonalUOController {

    private final IPersonalUOService personalUOService;

    public PersonalUOController(
            IPersonalUOService personalUOService
    ) {
        this.personalUOService =
                personalUOService;
    }

    @GetMapping("/buscarPersonalxUO/{codigoUo}")
    @Operation(
            summary = "Buscar personal por unidad orgánica",
            description = """
                    Lista personal activo

                    Tipo:
                    1 = Solo la unidad | 2 = Incluye unidades dependientes
                    codigoUo = 0 lista todo el personal.
                    """
    )
    public ResponseEntity<List<PersonalUO>> buscarPersonalxUO(

            @Parameter(
                    description = "Código de unidad orgánica",
                    example = "10296"
            )
            @PathVariable
            String codigoUo,

            @Parameter(
                    description = "1 = Solo la unidad, 2 = Toda la unidad.",
                    example = "1"
            )
            @RequestParam(
                    required = false,
                    defaultValue = "1"
            )
            Integer tipo
    ) {

        return ResponseEntity.ok(
                personalUOService.buscarPersonalxUO(
                        codigoUo,
                        tipo
                )
        );
    }
}
