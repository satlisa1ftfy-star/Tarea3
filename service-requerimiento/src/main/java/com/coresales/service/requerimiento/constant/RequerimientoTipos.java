package com.coresales.service.requerimiento.constant;

public final class RequerimientoTipos {

    private RequerimientoTipos() {
    }

    public enum TipoEvaluacion {
        POR_EVALUAR(6),
        NO_REQUIERE_AUTORIZACION(7),
        PENDIENTE_SOLICITAR_AUTORIZACION(8),
        POR_AUTORIZAR(9),
        AUTORIZADO(10),
        NO_AUTORIZADO(11);

        private final int codigo;
        TipoEvaluacion(int codigo) {
            this.codigo = codigo;
        }
        public int getCodigo() {
            return codigo;
        }
    }

    public enum OpcionAutorizar {
        SOLICITAR_AUTORIZA(1),
        AUTORIZAR(2),
        ANULAR_SOLICITUD_AUTORIZA(3);
        private final int codigo;
        OpcionAutorizar(int codigo) {
            this.codigo = codigo;
        }
        public int getCodigo() {
            return codigo;
        }
    }
    public enum EstadoRequerimiento {
        REGISTRADO(1),
        POR_ASIGNAR(2),
        ASIGNADO(3),
        EN_PROCESO(4),
        ATENDIDO(5);
        private final int codigo;
        EstadoRequerimiento(int codigo) {
            this.codigo = codigo;
        }
        public int getCodigo() {
            return codigo;
        }
    }

}