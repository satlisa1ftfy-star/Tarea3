package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.RequerimientoAsignado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RequerimientoAsignacionRepositoryImpl
        implements RequerimientoAsignacionRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RequerimientoAsignado asignar(
            Integer codigoRequerimiento,
            Integer codigoPersonaAsigna,
            String codigoPersonaResponsable,
            boolean responsablePrincipal,
            boolean informeTecnico,
            String observacion,
            String codigoPersonaActualizacion,
            String nombreTerminal
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siTipBus=\"5\" " +
                        "siTipOpe=\"1\" " +
                        "iCodigo_Req=\"" + codigoRequerimiento + "\" " +
                        "iCodigo_Per=\"" + codigoPersonaAsigna + "\" " +
                        "cCodPer=\"" + escaparXml(codigoPersonaResponsable) + "\" " +
                        "bResPrincipal_ReqMov=\"" + (responsablePrincipal ? "1" : "0") + "\" " +
                        "bInformeTecnico_ReqMov=\"" + (informeTecnico ? "1" : "0") + "\" " +
                        "vObservacion_ReqMov=\"" + escaparXml(observacion) + "\" " +
                        "cCodPerActualizacion=\"" + escaparXml(codigoPersonaActualizacion) + "\" " +
                        "cNombreTerminal_Req=\"" + escaparXml(nombreTerminal) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Requerimiento_Registrar"
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
        List<Object> resultados =
                query.getResultList();

        Integer codigoRequerimientoResultado = codigoRequerimiento;

        if (!resultados.isEmpty()) {

            Object fila = resultados.get(0);

            // El SP retorna 1 columna; según el driver, el resultado viene encapsulado
            // en un arreglo (Object[]) o como el valor escalar directo.
            Object valor =
                    fila instanceof Object[] arreglo
                            ? (arreglo.length > 0 ? arreglo[0] : null)
                            : fila;

            if (valor != null) {
                codigoRequerimientoResultado =
                        valor instanceof Number numero
                                ? numero.intValue()
                                : Integer.valueOf(valor.toString().trim());
            }
        }

        return new RequerimientoAsignado(
                codigoRequerimientoResultado,
                nombreTerminal
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
