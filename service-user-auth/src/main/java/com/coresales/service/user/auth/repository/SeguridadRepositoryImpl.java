package com.coresales.service.user.auth.repository;

import com.coresales.service.user.auth.model.UsuarioRolDetalle;
import com.coresales.service.user.auth.model.UsuarioSesion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SeguridadRepositoryImpl
        implements SeguridadRepository {

    private static final String STORED_PROCEDURE =
            "dbo.spGR_Seguridad_ConsultarUsuario";

    private static final String SP_REGISTRAR_INICIO_SESION =
            "dbo.spGR_Seguridad_RegistrarInicioSesion";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UsuarioSesion consultarUsuario(
            String usuarioWindows
    ) {

        String xml = construirXml(
                1,
                usuarioWindows,
                0
        );

        List<Object[]> resultados =
                ejecutar(xml);

        if (resultados.isEmpty()) {
            return null;
        }

        Object[] fila = resultados.get(0);

        UsuarioSesion usuarioSesion =
                new UsuarioSesion();

        usuarioSesion.setUsuarioWindows(usuarioWindows);

        // 0 - NumeroRoles
        usuarioSesion.setNumeroRoles(
                convertirInteger(fila[0])
        );

        // 1 - iCodigo_Per
        usuarioSesion.setCodigoPersonaGr(
                convertirInteger(fila[1])
        );

        // 2 - NombreCompleto
        usuarioSesion.setNombreCompleto(
                convertirString(fila[2])
        );

        // 3 - cCodPer
        usuarioSesion.setCodigoPersonaOrganizacion(
                convertirString(fila[3])
        );

        return usuarioSesion;
    }

    @Override
    public List<UsuarioRolDetalle> listarRolesUsuario(
            String usuarioWindows,
            Integer codigoRol
    ) {

        String xml = construirXml(
                2,
                usuarioWindows,
                codigoRol == null ? 0 : codigoRol
        );

        List<Object[]> resultados =
                ejecutar(xml);

        List<UsuarioRolDetalle> roles =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            UsuarioRolDetalle detalle =
                    new UsuarioRolDetalle();

            // 0  - iCodigo_Per
            detalle.setCodigoPersonaGr(convertirInteger(fila[0]));
            // 1  - CCODPER
            detalle.setCodigoPersonaOrganizacion(convertirString(fila[1]));
            // 2  - VUSUWIN
            detalle.setUsuarioWindows(convertirString(fila[2]));
            // 3  - VNUMDNI
            detalle.setNumeroDocumento(convertirString(fila[3]));
            // 4  - VNOMBRE
            detalle.setNombres(convertirString(fila[4]));
            // 5  - VAPEPAT
            detalle.setApellidoPaterno(convertirString(fila[5]));
            // 6  - VAPEMAT
            detalle.setApellidoMaterno(convertirString(fila[6]));
            // 7  - NCODUO
            detalle.setCodigoUnidadOrganica(convertirInteger(fila[7]));
            // 8  - VDESLUO
            detalle.setUnidadOrganica(convertirString(fila[8]));
            // 9  - NCODCAR
            detalle.setCodigoCargo(convertirInteger(fila[9]));
            // 10 - VDESCAR
            detalle.setCargo(convertirString(fila[10]));
            // 11 - VNOMEST
            detalle.setEstadoPersonal(convertirString(fila[11]));
            // 12 - VNOMCAT
            detalle.setCategoria(convertirString(fila[12]));
            // 13 - VCORREO
            detalle.setCorreo(convertirString(fila[13]));
            // 14 - siCodigo_Rol
            detalle.setCodigoRol(convertirInteger(fila[14]));
            // 15 - vNombre_Rol
            detalle.setNombreRol(convertirString(fila[15]));
            // 16 - iCodigo_PerRol
            detalle.setCodigoPersonaRol(convertirInteger(fila[16]));
            // 17 - iCodigo_TipoRol
            detalle.setCodigoTipoRol(convertirInteger(fila[17]));
            // 18 - vNombre_TipoRol
            detalle.setNombreTipoRol(convertirString(fila[18]));

            roles.add(detalle);
        }

        return roles;
    }

    @Override
    public Integer registrarInicioSesion(
            Integer codigoPersonaGr,
            Integer codigoPersonaRol,
            String codigoSesion,
            String codigoPersonaActualizacion,
            String nombreTerminal
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "iCodigo_Per=\"" + codigoPersonaGr + "\" " +
                        "iCodigo_PerRol=\"" + codigoPersonaRol + "\" " +
                        "vCodigoSesion_Ac=\"" + escaparXml(codigoSesion) + "\" " +
                        "cCodPerActualizacion=\"" + escaparXml(codigoPersonaActualizacion) + "\" " +
                        "cNombreTerminal_Ac=\"" + escaparXml(nombreTerminal) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        SP_REGISTRAR_INICIO_SESION
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

        // El SP devuelve una sola columna: iCodigo_Ing
        List<?> resultados = query.getResultList();

        if (resultados.isEmpty()) {
            return null;
        }

        Object valor = resultados.get(0);

        if (valor instanceof Object[] fila) {
            valor = fila.length > 0 ? fila[0] : null;
        }

        return convertirInteger(valor);
    }

    private List<Object[]> ejecutar(String xml) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        STORED_PROCEDURE
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

        return resultados;
    }

    private String construirXml(
            int tipoBusqueda,
            String usuarioWindows,
            int codigoRol
    ) {

        return "<R>" +
                "<XmlGR " +
                "siTipBus=\"" + tipoBusqueda + "\" " +
                "vUsuWin=\"" + escaparXml(usuarioWindows) + "\" " +
                "siCodigo_Rol=\"" + codigoRol + "\" " +
                "/>" +
                "</R>";
    }

    private Integer convertirInteger(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        return Integer.valueOf(valor.toString());
    }

    private String convertirString(Object valor) {

        if (valor == null) {
            return null;
        }

        return valor.toString().trim();
    }

    private String escaparXml(String valor) {

        if (valor == null) {
            return "";
        }

        return valor
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&apos;");
    }
}
