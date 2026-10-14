package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.constant.RequerimientoTipos;
import com.coresales.service.requerimiento.model.AutorizarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.util.XmlBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
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
        // siTipBus = 2: el SP actualiza la fila de la persona que autoriza
        // (iCodigo_Per) y usa iCodigo_TipEva 10 = Autorizado / 11 = No autorizado.
        RequerimientoTipos.TipoEvaluacion evaluacion =
                Boolean.FALSE.equals(solicitud.getAutoriza())
                        ? RequerimientoTipos.TipoEvaluacion.NO_AUTORIZADO
                        : RequerimientoTipos.TipoEvaluacion.AUTORIZADO;

        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("siTipBus", RequerimientoTipos.OpcionAutorizar.AUTORIZAR.getCodigo());
        atributos.put("iCodigo_Req", solicitud.getCodRequerimiento());
        atributos.put("iCodigo_Per", solicitud.getCodPerHelpDesk());
        atributos.put("iCodigo_TipEva", evaluacion.getCodigo());
        atributos.put("vObservacion_ReqMov", solicitud.getObservacion());
        atributos.put("cCodPerActualizacion", solicitud.getCodPerActualiza());
        atributos.put("cNombreTerminal_PerAut", solicitud.getNomTerminal());
        String xmlGR = XmlBuilder.elemento("XmlGR", atributos);
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