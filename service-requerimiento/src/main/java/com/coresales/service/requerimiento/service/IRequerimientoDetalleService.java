package com.coresales.service.requerimiento.service;

import com.coresales.service.requerimiento.model.DocumentoAdjuntoDetalle;
import com.coresales.service.requerimiento.model.HistorialRequerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalle;

import java.util.List;

public interface IRequerimientoDetalleService {

    RequerimientoDetalle consultarDetalle(Integer codigoRequerimiento);

    List<HistorialRequerimiento> consultarHistorial(Integer codigoRequerimiento);

    List<DocumentoAdjuntoDetalle> consultarAdjuntos(Integer codigoRequerimiento);
}
