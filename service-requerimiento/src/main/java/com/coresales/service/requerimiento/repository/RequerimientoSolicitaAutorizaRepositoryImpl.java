package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.constant.RequerimientoTipos;
import com.coresales.service.requerimiento.dto.response.ConsultaSolicitudAutorizaResponse;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;
import com.coresales.service.requerimiento.util.XmlBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.coresales.service.requerimiento.constant.RequerimientoConstants.*;

@Repository
public class RequerimientoSolicitaAutorizaRepositoryImpl
        implements RequerimientoSolicitaAutorizaRepository {
    private static final String SP_SOLICITUD ="dbo.spGR_Requerimiento_RegistrarPersonaAutorizacion";
    private static final String SP_SOLICITUD_CONSULTA ="dbo.spGR_Requerimiento_ConsultarPersonaAutorizacion";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RequerimientoResultado solicitarAutorizacion(
            SolicitarAutorizaRequerimientoRequest solicitud
    ) {
        // LinkedHashMap: admite nulos (Map.of lanza NullPointerException).
        Map<String, Object> atributos = new LinkedHashMap<>();
        atributos.put("siTipBus", RequerimientoTipos.OpcionAutorizar.SOLICITAR_AUTORIZA.getCodigo());
        atributos.put("iCodigo_Req", solicitud.getCodRequerimiento());
        atributos.put("iCodigo_Per", solicitud.getCodPerHelpDesk());
        atributos.put("iCodigo_TipEva", RequerimientoTipos.TipoEvaluacion.POR_AUTORIZAR.getCodigo());
        atributos.put("iCodigo_PerSol_PerAut", solicitud.getCodPerSolAutorizacion());
        atributos.put("iCodigo_PerAnul_PerAut", solicitud.getCodPerSolAnulacion());
        atributos.put("vCodPerAutorizacion", solicitud.getCodPerAutoriza());
        atributos.put("vObservacion_ReqMov", solicitud.getObservacion());
        atributos.put("cCodPerActualizacion", solicitud.getCodPerActualiza());
        atributos.put("cNombreTerminal_PerAut", solicitud.getNomTerminal());
        String xmlGR = XmlBuilder.elemento("XmlGR", atributos);
        String xml = "<R>" + xmlGR + "</R>";
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(SP_SOLICITUD);

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

    @Override
    public List<ConsultaSolicitudAutorizaResponse> consultarSolicitud(
            String tipBus, Integer codigoRequerimiento) {
        String xmlGR = XmlBuilder.elemento(
                "XmlGR",
                Map.of(
                        "siTipBus", tipBus,
                        "iCodigo_Req", codigoRequerimiento
                )
        );
        String xml = "<R>" + xmlGR + "</R>";
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(SP_SOLICITUD_CONSULTA);

        query.registerStoredProcedureParameter(
                "ptXmlGR",
                String.class,
                ParameterMode.IN
        );
        query.setParameter("ptXmlGR", xml);
        @SuppressWarnings("unchecked")
        List<Object[]> resultados = query.getResultList();

        List<ConsultaSolicitudAutorizaResponse> resAut =new ArrayList<>();

        for (Object[] fila : resultados) {

            ConsultaSolicitudAutorizaResponse sol=new ConsultaSolicitudAutorizaResponse();
            sol.setCodigo(fila[0] == null? null: fila[0].toString().trim());
            sol.setNombre(fila[1] == null? null: fila[1].toString().trim());
            sol.setEstado(fila[2] == null? null: fila[2].toString().trim());

            resAut.add(sol);
        }
        return resAut;
    }
}