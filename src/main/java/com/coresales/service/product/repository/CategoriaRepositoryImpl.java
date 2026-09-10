package com.coresales.service.product.repository;

import com.coresales.service.product.model.Categoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;


@Repository
public class CategoriaRepositoryImpl
        implements CategoriaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Categoria> buscar(
            String nombre,
            Integer codigoUo
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "vNombreCat=\"" + escaparXml(nombre) + "\" " +
                        "iCodUo=\"" + codigoUo + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Categoria_BuscarNombreUO"
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

        List<Categoria> categorias =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            Categoria categoria =
                    new Categoria();

            categoria.setCodigoCategoria(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            categoria.setNombreCategoria(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            categorias.add(categoria);
        }

        return categorias;
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