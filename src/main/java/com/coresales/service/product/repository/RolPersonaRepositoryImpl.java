package com.coresales.service.product.repository;


import com.coresales.service.product.model.RolPersona;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class RolPersonaRepositoryImpl
        implements RolPersonaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RolPersona> listarPorPersona(
            Integer codigoPer
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siTipBus=\"1\" " +
                        "iCodigo_Per=\"" + codigoPer + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_PersonaRol_Consultar"
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

        List<RolPersona> roles =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            RolPersona rol =
                    new RolPersona();

            // fila[0] -> siCodigo_Rol
            rol.setCodigoRol(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            // fila[1] -> vNombre_Rol
            rol.setNombreRol(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );

            // fila[2] -> Estado
            rol.setEstado(
                    fila[2] == null
                            ? null
                            : fila[2].toString().trim()
            );

            roles.add(rol);
        }

        return roles;
    }
}
