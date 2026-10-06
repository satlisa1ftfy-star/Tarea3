package com.coresales.service.requerimiento.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CargoResponse {

    private Integer codigoCargo;
    private String nombreCargo;
    private String tipAprob;
    private Integer codigo_EvalCar;
}