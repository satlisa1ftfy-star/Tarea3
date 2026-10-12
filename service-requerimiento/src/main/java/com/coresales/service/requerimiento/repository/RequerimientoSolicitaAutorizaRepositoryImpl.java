package com.coresales.service.requerimiento.repository;
import com.coresales.service.requerimiento.constant.RequerimientoTipos;
import com.coresales.service.requerimiento.dto.response.ConsultaSolicitudAutorizaResponse;
import com.coresales.service.requerimiento.model.RequerimientoResultado;
import com.coresales.service.requerimiento.model.RolPersona;
import com.coresales.service.requerimiento.model.SolicitarAutorizaRequerimientoRequest;
import com.coresales.service.requerimiento.util.XmlBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
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
        RequerimientoResultado oRes=new RequerimientoResultado();
        String xmlGR = XmlBuilder.elemento(
                "XmlGR",
                Map.of(
                        "siTipBus", RequerimientoTipos.OpcionAutorizar.SOLICITAR_AUTORIZA.getCodigo(),
                        "iCodigo_Req", solicitud.getCodRequerimiento(),
                        "iCodigo_Per", solicitud.getCodPerHelpDesk(),
                        "iCodigo_TipEva", RequerimientoTipos.TipoEvaluacion.POR_AUTORIZAR.getCodigo(),
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

            Object valor = resultados.get(0);
            ConsultaSolicitudAutorizaResponse sol=new ConsultaSolicitudAutorizaResponse();
            sol.setCodigo(fila[0] == null? null: fila[0].toString().trim());
            sol.setNombre(fila[1] == null? null: fila[1].toString().trim());
            sol.setEstado(fila[2] == null? null: fila[2].toString().trim());

            resAut.add(sol);
        }
        return resAut;
    }
}