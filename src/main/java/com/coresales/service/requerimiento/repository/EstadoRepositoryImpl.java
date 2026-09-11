package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.Estado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class EstadoRepositoryImpl
        implements EstadoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Estado> buscarPorNombre(
            String nombre
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "vNombreEst=\"" + escaparXml(nombre) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Estado_BuscarNombre"
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

        List<Estado> estados =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            Estado estado =
                    new Estado();

            // fila[0] -> siCodigo_Est
            estado.setCodigoEstado(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            // fila[1] -> vNombre_Est
            estado.setNombreEstado(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            estados.add(estado);
        }

        return estados;
    }

    private String escaparXml(
            String valor
    ) {

        return valor
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }
}