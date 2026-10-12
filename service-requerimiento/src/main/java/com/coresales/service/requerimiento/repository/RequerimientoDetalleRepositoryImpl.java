package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.CorreoCopia;
import com.coresales.service.requerimiento.model.DocumentoAdjuntoDetalle;
import com.coresales.service.requerimiento.model.HistorialRequerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalle;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lectura del detalle e historial (mismo XML @ptXmlGR).
 * siTipBus = 3
 * Se usa JDBC (CallableStatement) en vez de StoredProcedureQuery porque el detalle
 * devuelve DOS result sets (datos + correos copia) y conviene leer las columnas por nombre.
 */
@Repository
public class RequerimientoDetalleRepositoryImpl
        implements RequerimientoDetalleRepository {

    private final JdbcTemplate jdbcTemplate;

    public RequerimientoDetalleRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RequerimientoDetalle consultarDetalle(Integer codigoRequerimiento) {

        String xml = "<R><XmlGR iCodigo_Req=\"" + codigoRequerimiento + "\" /></R>";

        return jdbcTemplate.execute(
                "{call dbo.spGR_Requerimiento_ConsultarDetalle(?)}",
                (CallableStatementCallback<RequerimientoDetalle>) cs -> {
                    cs.setString(1, xml);

                    RequerimientoDetalle detalle = null;
                    List<CorreoCopia> correos = new ArrayList<>();
                    int conjunto = 0;

                    // Recorre los result sets (ignora los conteos de filas de las tablas temporales).
                    boolean esResultSet = cs.execute();
                    while (true) {
                        if (esResultSet) {
                            try (ResultSet rs = cs.getResultSet()) {
                                Set<String> columnas = columnas(rs);
                                if (conjunto == 0) {
                                    // Puede haber varias filas (un movimiento por asignación): la 1ra es la vigente.
                                    if (rs.next()) {
                                        detalle = mapearDetalle(rs, columnas);
                                    }
                                } else if (conjunto == 1) {
                                    while (rs.next()) {
                                        correos.add(new CorreoCopia(
                                                texto(rs, columnas, "vUsuWin"),
                                                texto(rs, columnas, "vNombre"),
                                                texto(rs, columnas, "vDesCar")
                                        ));
                                    }
                                }
                            }
                            conjunto++;
                        } else if (cs.getUpdateCount() == -1) {
                            break;
                        }
                        esResultSet = cs.getMoreResults();
                    }

                    // Respaldo: si el requerimiento no existe no hay fila de datos.
                    // (El SP nuevo usa LEFT JOIN, así que un requerimiento sin clasificar sí trae datos.)
                    if (detalle == null) {
                        detalle = new RequerimientoDetalle();
                        detalle.setCodigoRequerimiento(codigoRequerimiento);
                    }
                    detalle.setCorreosCopia(correos);
                    return detalle;
                }
        );
    }

    @Override
    public List<HistorialRequerimiento> consultarHistorial(Integer codigoRequerimiento) {

        String xml = "<R><XmlGR siTipBus=\"1\" iCodigo_Req=\"" + codigoRequerimiento + "\" /></R>";

        return jdbcTemplate.execute(
                "{call dbo.spGR_Requerimiento_ConsultarHistorial(?)}",
                (CallableStatementCallback<List<HistorialRequerimiento>>) cs -> {
                    cs.setString(1, xml);
                    List<HistorialRequerimiento> historial = new ArrayList<>();

                    boolean esResultSet = cs.execute();
                    while (true) {
                        if (esResultSet) {
                            try (ResultSet rs = cs.getResultSet()) {
                                Set<String> columnas = columnas(rs);
                                while (rs.next()) {
                                    historial.add(new HistorialRequerimiento(
                                            entero(rs, columnas, "Nro"),
                                            entero(rs, columnas, "Movimiento"),
                                            fecha(rs, columnas, "Fecha"),
                                            texto(rs, columnas, "Estado"),
                                            texto(rs, columnas, "PerOrigen"),
                                            texto(rs, columnas, "UniOrgOrigen"),
                                            texto(rs, columnas, "PerDestino"),
                                            texto(rs, columnas, "UniOrgDestino"),
                                            // la columna se llama "Observación" (con tilde)
                                            texto(rs, columnas, "Observación"),
                                            booleano(rs, columnas, "Vigencia")
                                    ));
                                }
                            }
                            break; // un solo result set
                        } else if (cs.getUpdateCount() == -1) {
                            break;
                        }
                        esResultSet = cs.getMoreResults();
                    }
                    return historial;
                }
        );
    }

    @Override
    public List<DocumentoAdjuntoDetalle> consultarAdjuntos(Integer codigoRequerimiento) {

        // SP del legado sin cambios (archivo inicial del registro + GRMovDocumentoAdjunto).
        String xml = "<R><XmlGR siTipBus=\"1\" iCodigo_Req=\"" + codigoRequerimiento + "\" /></R>";

        return jdbcTemplate.execute(
                "{call dbo.spGR_Requerimiento_ConsultarDocumentoAdjunto(?)}",
                (CallableStatementCallback<List<DocumentoAdjuntoDetalle>>) cs -> {
                    cs.setString(1, xml);
                    List<DocumentoAdjuntoDetalle> adjuntos = new ArrayList<>();

                    boolean esResultSet = cs.execute();
                    while (true) {
                        if (esResultSet) {
                            try (ResultSet rs = cs.getResultSet()) {
                                Set<String> columnas = columnas(rs);
                                while (rs.next()) {
                                    adjuntos.add(new DocumentoAdjuntoDetalle(
                                            entero(rs, columnas, "iNumCor"),
                                            entero(rs, columnas, "iCodigo_DocAdj"),
                                            texto(rs, columnas, "TipoAdjunto"),
                                            texto(rs, columnas, "vNombre_DocAdj"),
                                            texto(rs, columnas, "vNombreOriginal_DocAdj"),
                                            texto(rs, columnas, "vDescripcion_DocAdj"),
                                            texto(rs, columnas, "Usuario"),
                                            fechaTexto(rs, columnas, "sdFechaCarga_DocAdj")
                                    ));
                                }
                            }
                            break; // un solo result set
                        } else if (cs.getUpdateCount() == -1) {
                            break;
                        }
                        esResultSet = cs.getMoreResults();
                    }
                    return adjuntos;
                }
        );
    }

    private RequerimientoDetalle mapearDetalle(ResultSet rs, Set<String> c) throws SQLException {

        RequerimientoDetalle d = new RequerimientoDetalle();

        d.setCodigoRequerimiento(entero(rs, c, "iCodigo_Req"));
        d.setCodigoRequerimientoOrigen(entero(rs, c, "iCodigoPadre_Req"));
        d.setFechaRegistro(fecha(rs, c, "sdFecha_Req"));
        d.setEstado(texto(rs, c, "vNombre_Est"));
        d.setPrioridad(texto(rs, c, "Prioridad"));
        d.setCategoria(texto(rs, c, "vNombre_Cat"));
        d.setSubcategoria(texto(rs, c, "vNombre_SubCat"));
        d.setClasificacion(texto(rs, c, "vDescripcion_Cla"));
        d.setDescripcion(texto(rs, c, "vDescripcion_Req"));
        d.setObservacion(texto(rs, c, "vObservacion_Req"));

        d.setUnidadOrganicaSolicitante(texto(rs, c, "vDesLUo"));
        d.setSolicitante(texto(rs, c, "Solicitante"));
        d.setCargoSolicitante(texto(rs, c, "vDesCar"));

        d.setAsignadoPor(texto(rs, c, "ResponsableAdministrador"));
        d.setResponsable(texto(rs, c, "Responsable"));
        // El SP devuelve '(Principal)' o '' en esta columna.
        String principal = texto(rs, c, "bResPrincipal_ReqMov");
        d.setResponsablePrincipal(principal != null && principal.toLowerCase().contains("principal"));
        d.setCargoResponsable(texto(rs, c, "CargoResponsable"));
        d.setIndicadorAvance(booleano(rs, c, "bAvance_Req"));

        d.setUbicacionActual(texto(rs, c, "Ubicacion"));
        d.setTipoEvaluacion(texto(rs, c, "TipoEvaluacion"));
        d.setMedioEnvio(texto(rs, c, "MedioEnvio"));
        d.setTipoDocumentoReferencia(texto(rs, c, "DocSustento"));
        d.setDocumentoReferencia(texto(rs, c, "cNumero_DocSus"));
        d.setFechaDocumentoReferencia(texto(rs, c, "sdFecha_DocSus"));
        d.setDocumentoFisico(booleano(rs, c, "bFisico_Req"));

        return d;
    }

    // ---- lectura segura por nombre (null si la columna no existe en esta versión del SP) ----

    private Set<String> columnas(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        Set<String> nombres = new HashSet<>();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            nombres.add(meta.getColumnLabel(i).toLowerCase());
        }
        return nombres;
    }

    private String texto(ResultSet rs, Set<String> c, String columna) throws SQLException {
        if (!c.contains(columna.toLowerCase())) return null;
        String valor = rs.getString(columna);
        if (valor == null) return null;
        valor = valor.trim();
        return valor.isEmpty() ? null : valor;
    }

    private Integer entero(ResultSet rs, Set<String> c, String columna) throws SQLException {
        if (!c.contains(columna.toLowerCase())) return null;
        int valor = rs.getInt(columna);
        return rs.wasNull() ? null : valor;
    }

    private Boolean booleano(ResultSet rs, Set<String> c, String columna) throws SQLException {
        if (!c.contains(columna.toLowerCase())) return null;
        boolean valor = rs.getBoolean(columna);
        return rs.wasNull() ? null : valor;
    }

    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // La fecha puede venir como datetime o ya como texto (Convert(Char(10), ..., 103)) según la versión del SP.
    private String fechaTexto(ResultSet rs, Set<String> c, String columna) throws SQLException {
        if (!c.contains(columna.toLowerCase())) return null;
        Object valor = rs.getObject(columna);
        if (valor == null) return null;
        if (valor instanceof Timestamp ts) return ts.toLocalDateTime().format(FORMATO_FECHA_HORA);
        if (valor instanceof LocalDateTime ldt) return ldt.format(FORMATO_FECHA_HORA);
        String texto = valor.toString().trim();
        return texto.isEmpty() ? null : texto;
    }

    private LocalDateTime fecha(ResultSet rs, Set<String> c, String columna) throws SQLException {
        if (!c.contains(columna.toLowerCase())) return null;
        Timestamp valor = rs.getTimestamp(columna);
        return valor == null ? null : valor.toLocalDateTime();
    }
}
