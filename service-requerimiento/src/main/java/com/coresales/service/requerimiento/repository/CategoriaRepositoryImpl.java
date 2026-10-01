package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.model.CategoriaActivo;
import com.coresales.service.requerimiento.model.Categoria;
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

    /** spGR_Categoria_Consultar siTipBus=3: 0 siCodigo_Cat, 1 vnombre_Cat, 3 vNombre_Act, 7 siCodigo_Act. */
    @Override
    @SuppressWarnings("unchecked")
    public List<CategoriaActivo> buscarConActivo(Integer codigoUo) {
        String xml = "<R><XmlGR siTipBus=\"3\" siCodigo_Cat=\"0\" iCodUo=\"" + (codigoUo == null ? 0 : codigoUo)
                + "\" siCodigo_Act=\"0\" vNombre_Cat=\"\" bActivo_Cat=\"1\" /></R>";

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("dbo.spGR_Categoria_Consultar");
        query.registerStoredProcedureParameter("ptXmlGR", String.class, ParameterMode.IN);
        query.setParameter("ptXmlGR", xml);

        List<CategoriaActivo> categorias = new ArrayList<>();
        for (Object[] fila : (List<Object[]>) query.getResultList()) {
            CategoriaActivo c = new CategoriaActivo();
            c.setCodigoCategoria(fila[0] == null ? null : ((Number) fila[0]).intValue());
            c.setNombreCategoria(fila[1] == null ? null : fila[1].toString().trim());
            c.setNombreActivo(fila.length > 3 && fila[3] != null ? fila[3].toString().trim() : null);
            c.setCodigoActivo(fila.length > 7 && fila[7] != null ? ((Number) fila[7]).intValue() : null);
            categorias.add(c);
        }
        return categorias;
    }
}
