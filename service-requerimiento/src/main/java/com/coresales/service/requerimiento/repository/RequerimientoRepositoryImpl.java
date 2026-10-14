package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.Requerimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RequerimientoRepositoryImpl
        implements RequerimientoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Requerimiento> buscar(
            Integer numero,
            String titulo,
            String fechaInicio,
            String fechaFin,
            Integer codigoPersonaSolicitante,
            Integer codigoUoSolicitante,
            Integer codigoPersonaResponsable,
            Integer codigoUoResponsable,
            Integer codigoUoRequerimiento,
            Integer codigoEstado,
            Integer vigencia
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "iCodigoReq=\"" + numero + "\" " +
                        "vTituloReq=\"" + escaparXml(titulo) + "\" " +
                        "cFechaInicialReq=\"" + escaparXml(fechaInicio) + "\" " +
                        "cFechaFinalReq=\"" + escaparXml(fechaFin) + "\" " +
                        "iCodigoPerSolicitante=\"" + codigoPersonaSolicitante + "\" " +
                        "iCodUoSolicitante=\"" + codigoUoSolicitante + "\" " +
                        "iCodigoPerResponsable=\"" + codigoPersonaResponsable + "\" " +
                        "iCodUoResponsable=\"" + codigoUoResponsable + "\" " +
                        "iCodUoRequerimiento=\"" + codigoUoRequerimiento + "\" " +
                        "siCodigoEst=\"" + codigoEstado + "\" " +
                        "siVigencia=\"" + vigencia + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Requerimiento_BuscarFiltros"
                );

        query.registerStoredProcedureParameter(
                "ptXmlGR",
                String.class,
                ParameterMode.IN
        );

        query.setParameter(
                "ptXmlGR",
                xml
        );

        @SuppressWarnings("unchecked")
        List<Object[]> resultados =
                query.getResultList();

        List<Requerimiento> requerimientos =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            Requerimiento requerimiento =
                    new Requerimiento();

            // 0 - codigoRequerimiento
            requerimiento.setCodigoRequerimiento(
                    convertirInteger(fila[0])
            );

            // 1 - titulo
            requerimiento.setTitulo(
                    convertirString(fila[1])
            );

            // 2 - fechaRegistro
            requerimiento.setFechaRegistro(
                    convertirFecha(fila[2])
            );

            // 3 - descripcion
            requerimiento.setDescripcion(
                    convertirString(fila[3])
            );

            // 4 - codigoPersonaSolicitante
            requerimiento.setCodigoPersonaSolicitante(
                    convertirInteger(fila[4])
            );

            // 5 - solicitante
            requerimiento.setSolicitante(
                    convertirString(fila[5])
            );

            // 6 - codigoUoSolicitante
            requerimiento.setCodigoUoSolicitante(
                    convertirInteger(fila[6])
            );

            // 7 - unidadOrganicaSolicitante
            requerimiento.setUnidadOrganicaSolicitante(
                    convertirString(fila[7])
            );

            // 8 - codigoCargoSolicitante
            requerimiento.setCodigoCargoSolicitante(
                    convertirInteger(fila[8])
            );

            // 9 - cargoSolicitante
            requerimiento.setCargoSolicitante(
                    convertirString(fila[9])
            );

            // 10 - codigoPersonaResponsable
            requerimiento.setCodigoPersonaResponsable(
                    convertirInteger(fila[10])
            );

            // 11 - responsable
            requerimiento.setResponsable(
                    convertirString(fila[11])
            );

            // 12 - codigoUoResponsable
            requerimiento.setCodigoUoResponsable(
                    convertirInteger(fila[12])
            );

            // 13 - unidadOrganicaResponsable
            requerimiento.setUnidadOrganicaResponsable(
                    convertirString(fila[13])
            );

            // 14 - codigoCargoResponsable
            requerimiento.setCodigoCargoResponsable(
                    convertirInteger(fila[14])
            );

            // 15 - cargoResponsable
            requerimiento.setCargoResponsable(
                    convertirString(fila[15])
            );

            // 16 - codigoEstado
            requerimiento.setCodigoEstado(
                    convertirInteger(fila[16])
            );

            // 17 - estado
            requerimiento.setEstado(
                    convertirString(fila[17])
            );

            // 18 - codigoCategoria
            requerimiento.setCodigoCategoria(
                    convertirInteger(fila[18])
            );

            // 19 - categoria
            requerimiento.setCategoria(
                    convertirString(fila[19])
            );

            // 20 - codigoSubcategoria
            requerimiento.setCodigoSubcategoria(
                    convertirInteger(fila[20])
            );

            // 21 - subcategoria
            requerimiento.setSubcategoria(
                    convertirString(fila[21])
            );

            // 22 - codigoPrioridad
            requerimiento.setCodigoPrioridad(
                    convertirInteger(fila[22])
            );

            // 23 - prioridad
            requerimiento.setPrioridad(
                    convertirString(fila[23])
            );

            // 24 - codigoUoRequerimiento
            requerimiento.setCodigoUoRequerimiento(
                    convertirInteger(fila[24])
            );

            // 25 - unidadOrganicaRequerimiento
            requerimiento.setUnidadOrganicaRequerimiento(
                    convertirString(fila[25])
            );

            // 26 - codigoCategoriaSubcategoria
            requerimiento.setCodigoCategoriaSubcategoria(
                    convertirInteger(fila[26])
            );

            requerimientos.add(requerimiento);
        }

        return requerimientos;
    }

    private Integer convertirInteger(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        return Integer.valueOf(
                valor.toString()
        );
    }

    private String convertirString(Object valor) {

        if (valor == null) {
            return null;
        }

        return valor.toString().trim();
    }

    private LocalDateTime convertirFecha(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (valor instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        throw new IllegalArgumentException(
                "No se pudo convertir el valor de fecha: "
                        + valor
        );
    }

    private String escaparXml(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }
}
