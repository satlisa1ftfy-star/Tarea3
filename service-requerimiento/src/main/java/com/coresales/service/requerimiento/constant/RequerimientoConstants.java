package com.coresales.service.requerimiento.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public final class RequerimientoConstants {
    private RequerimientoConstants() {
    }
    public static final int RESULTADO_OK = 1;
    public static final int RESULTADO_ERROR = -1;

    public static final int NUMERO_DOS = 2;
    public static final int NUMERO_UNO = 1;

    public static final int TIPO_EVALUACION_ = 1;
    public static final int NUMERO_UNO = 1;

}


@Getter
@RequiredArgsConstructor
public enum TipoEvaluacion {
    POR_EVALUAR(6),
    NO_REQUIERE_AUTORIZACION(7),
    PENDIENTE_SOLICITAR_AUTORIZACION(8),
    POR_AUTORIZAR(9),
    AUTORIZADO(10),
    NO_AUTORIZADO(11);
    private final int codigo;
}
