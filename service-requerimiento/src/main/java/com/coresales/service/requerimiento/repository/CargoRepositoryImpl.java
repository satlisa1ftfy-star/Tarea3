package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.dto.response.CargoResponse;
import com.coresales.service.requerimiento.model.Categoria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;


@Repository
public class CargoRepositoryImpl
        implements CargoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<CargoResponse> buscar(
            Integer tipBus,
            Integer codigoReq,
            Integer codigoCat,
            Integer codigo_SubCat
    ) {
        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siTipBus=\"" + tipBus + "\" " +
                        "iCodigo_Req=\"" + codigoReq + "\" " +
                        "siCodigo_Cat=\"" + codigoCat + "\" " +
                        "siCodigo_SubCat=\"" + codigo_SubCat + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Requerimiento_ConsultarGrupoCargos"
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

        List<CargoResponse> listaCargos =
                new ArrayList<>();
        CargoResponse cargos;

        for (Object[] fila : resultados) {

            cargos =new CargoResponse();
            cargos.setCodigoCargo(
                    fila[0] == null
                            ? null
                            : ((Number) fila[0]).intValue()
            );

            cargos.setNombreCargo(
                    fila[1] == null
                            ? null
                            : fila[1].toString().trim()
            );
            cargos.setTipAprob(
                    fila[6] == null
                            ? null
                            : fila[6].toString().trim()
            );
            cargos.setCodigo_EvalCar(
                    fila[5] == null
                            ? null
                            : ((Number) fila[5]).intValue()
            );
            System.out.println("cargos");
            System.out.println(cargos);
            listaCargos.add(cargos);
        }

        return listaCargos;
    }
}