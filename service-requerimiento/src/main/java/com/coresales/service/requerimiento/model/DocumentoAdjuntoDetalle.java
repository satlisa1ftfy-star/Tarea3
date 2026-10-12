package com.coresales.service.requerimiento.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//  siTipBus = 1.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoAdjuntoDetalle {

    private Integer numero;
    private Integer codigoDocumento;   // 0 = archivo cargado en el registro del requerimiento
    private String tipoAdjunto;
    private String nombreArchivo;      // nombre con el que se guardó
    private String nombreOriginal;
    private String descripcion;
    private String usuario;
    private String fechaCarga;         // dd/MM/yyyy [HH:mm:ss], tal como lo entrega el SP
}
