package com.coresales.service.product.repository;

import com.coresales.service.product.model.Persona;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class PersonaRepositoryImpl
        implements PersonaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Persona> listarPersonas(
            String nombre,
            Integer codigoUo,
            Integer vigencia
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "vNombre=\"" + escaparXml(nombre) + "\" " +
                        "iCodUo=\"" + codigoUo + "\" " +
                        "siVigencia=\"" + vigencia + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Persona_ConsultarNombreUO"
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

        List<Persona> personas =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            Persona persona =
                    new Persona();

            // 0 - iCodigo_Per
            persona.setCodigoPer(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            // 1 - Unidad orgánica
            persona.setUnidadOrganica(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            // 2 - Nombre
            persona.setNombre(
                    fila[2] == null
                            ? null
                            : fila[2].toString().trim()
            );

            // 3 - Observación
            persona.setObservacion(
                    fila[3] == null
                            ? null
                            : fila[3].toString().trim()
            );

            // 4 - Descripción vigencia
            persona.setVigencia(
                    fila[4] == null
                            ? null
                            : fila[4].toString().trim()
            );

            // 5 - Código UO
            persona.setCodigoUo(
                    fila[5] == null
                            ? null
                            : ((Number) fila[5]).intValue()
            );

            // 6 - Código persona
            persona.setCodigoPersona(
                    fila[6] == null
                            ? null
                            : fila[6].toString().trim()
            );

            // 7 - Vigente boolean
            persona.setVigente(
                    convertirBoolean(fila[7])
            );

            // 8 - Código cargo
            persona.setCodigoCargo(
                    fila[8] == null
                            ? null
                            : ((Number) fila[8]).intValue()
            );

            // 9 - Cargo
            persona.setCargo(
                    fila[9] == null
                            ? null
                            : fila[9].toString().trim()
            );

            personas.add(persona);
        }

        return personas;
    }

    private Boolean convertirBoolean(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Boolean booleano) {
            return booleano;
        }

        if (valor instanceof Number numero) {
            return numero.intValue() == 1;
        }

        return "1".equals(valor.toString())
                || Boolean.parseBoolean(valor.toString());
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