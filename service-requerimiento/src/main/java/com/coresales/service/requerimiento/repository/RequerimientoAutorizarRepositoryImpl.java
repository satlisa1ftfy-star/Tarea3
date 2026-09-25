package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.util.XmlBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

import static com.coresales.service.requerimiento.constant.RequerimientoConstants.*;

@Repository
public class RequerimientoAutorizarRepositoryImpl
        implements RequerimientoAutorizarRepository {
    private static final String SP_AUTORIZAR ="dbo.spGR_Requerimiento_RegistrarPersonaAutorizacion";
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RequerimientoResultado autorizar(
            AutorizarRequerimientoRequest solicitud
    ) {
        RequerimientoResultado oRes=new RequerimientoResultado();
        String xmlGR = XmlBuilder.elemento(
                "XmlGR",
                Map.of(
                        "siTipBus", NUMERO_DOS,
                        "iCodigo_Req", solicitud.getCodRequerimiento(),
                        "iCodigo_Per", solicitud.getCodPerHelpDesk(),
                        "iCodigo_TipEva", solicitud.getCodTipEvaluacion().toString(),
                        "iCodigo_PerSol_PerAut", solicitud.getCodPerSolAutorizacion().toString(),
                        "iCodigo_PerAnul_PerAut", solicitud.getCodPerSolAutorizacion().toString(),
                        "vCodPerAutorizacion", solicitud.getCodPerAutoriza(),
                        "vObservacion_ReqMov", solicitud.getObservacion(),
                        "cCodPerActualizacion", solicitud.getCodPerActualiza(),
                        "cNombreTerminal_PerAut",  solicitud.getNomTerminal()
                )
        );
        String xml = "<R>" + xmlGR + "</R>";
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(SP_AUTORIZAR);

        query.registerStoredProcedureParameter(
                "ptXmlGR",
                String.class,
                ParameterMode.IN
        );
        query.setParameter("ptXmlGR", xml);
        @SuppressWarnings("unchecked")
        List<Object> resultados = query.getResultList();
        RequerimientoResultado resultado = new RequerimientoResultado();

        if (!resultados.isEmpty()) {
            Object valor = resultados.get(0);

            if (valor instanceof Number numero) {
                resultado.setValorInt(numero.intValue());
                resultado.setCodigoResultado(RESULTADO_OK);
            }
        }
        return resultado;
    }
}