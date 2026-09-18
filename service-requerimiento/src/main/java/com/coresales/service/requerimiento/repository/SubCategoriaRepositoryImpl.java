package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.SubCategoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SubCategoriaRepositoryImpl
        implements SubCategoriaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<SubCategoria> buscar(
            Integer codigoCategoria,
            String nombre
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siCodigo_Cat=\"" + codigoCategoria + "\" " +
                        "vNombreSubCat=\"" + escaparXml(nombre) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_SubCategoria_BuscarNombreCategoria"
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

        List<SubCategoria> subcategorias =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            SubCategoria subcategoria =
                    new SubCategoria();

            subcategoria.setCodigoSubcategoria(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            subcategoria.setNombreSubcategoria(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            subcategorias.add(subcategoria);
        }

        return subcategorias;
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