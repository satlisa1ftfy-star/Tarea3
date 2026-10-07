package com.coresales.service.organizacion.repository;


import com.coresales.service.organizacion.model.UnidadOrganica;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class UnidadOrganicaRepositoryImpl
        implements UnidadOrganicaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<UnidadOrganica> buscarPorNombre(
            String nombre
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "vNombreUO=\"" + escaparXml(nombre) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_UnidadOrganica_BuscarNombre"
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

        List<UnidadOrganica> unidades =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            UnidadOrganica unidad =
                    new UnidadOrganica();

            // fila[0] -> codigoUo
            unidad.setCodigoUo(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            // fila[1] -> nombreUnidadOrganica
            unidad.setNombreUnidadOrganica(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            unidades.add(unidad);
        }

        return unidades;
    }

    private String escaparXml(String valor) {

        return valor
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }

    /** siTipBus=7: iCodigo_Uo, iCodUO, nNumGru, cCodPer, vSupUO, vDesLUO. El código devuelto es iCodUO. */
    @Override
    public List<UnidadOrganica> listarParaSolicitud() {
        return consultarUnidades("<R><XmlGR siTipBus=\"7\" iCodUo=\"0\" /></R>", 5);
    }

    /** siTipBus=2: iCodigo_Uo, iCodUO, vDesLUO, nNumGru, cCodPer, vSupUO. iCodUo = iCodUO del padre. */
    @Override
    public List<UnidadOrganica> buscarPorPadre(Integer codigoUoPadre) {
        return consultarUnidades("<R><XmlGR siTipBus=\"2\" iCodUo=\"" + codigoUoPadre + "\" /></R>", 2);
    }

    @SuppressWarnings("unchecked")
    private List<UnidadOrganica> consultarUnidades(String xml, int columnaNombre) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spGR_UnidadOrganica_Consultar");
        query.registerStoredProcedureParameter("ptXmlGR", String.class, ParameterMode.IN);
        query.setParameter("ptXmlGR", xml);

        List<UnidadOrganica> unidades = new ArrayList<>();
        for (Object[] fila : (List<Object[]>) query.getResultList()) {
            Object codigo = fila.length > 1 && fila[1] != null ? fila[1] : fila[0];
            Object nombre = fila.length > columnaNombre ? fila[columnaNombre] : null;
            UnidadOrganica u = new UnidadOrganica();
            u.setCodigoUo(codigo == null ? null : ((Number) codigo).intValue());
            // el SP antepone prefijos jerárquicos ("..|- ")
            u.setNombreUnidadOrganica(nombre == null ? null : nombre.toString().replaceFirst("^[\\s.|\\-]+", "").trim());
            unidades.add(u);
        }
        return unidades;
    }
}
