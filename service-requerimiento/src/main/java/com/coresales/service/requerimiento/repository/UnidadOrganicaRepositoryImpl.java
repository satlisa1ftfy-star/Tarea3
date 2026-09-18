package com.coresales.service.requerimiento.repository;


import com.coresales.service.requerimiento.model.UnidadOrganica;
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
}
