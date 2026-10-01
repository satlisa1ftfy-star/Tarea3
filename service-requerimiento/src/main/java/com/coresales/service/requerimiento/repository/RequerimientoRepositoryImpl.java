package com.coresales.service.requerimiento.repository;

import com.coresales.service.requerimiento.config.DocumentoStorage;
import com.coresales.service.requerimiento.model.ClasificarRequerimientoRequest;
import com.coresales.service.requerimiento.model.RegistrarRequerimientoRequest;
import com.coresales.service.requerimiento.model.Requerimiento;
import com.coresales.service.requerimiento.model.RequerimientoDetalleDTO;
import com.coresales.service.requerimiento.model.RequerimientoRegistroResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class RequerimientoRepositoryImpl
        implements RequerimientoRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Requerimiento> buscar(
            Integer numero,
            String titulo,
            String fechaInicio,
            String fechaFin,
            Integer codigoPersonaSolicitante,
            Integer codigoUoSolicitante,
            Integer codigoPersonaResponsable,
            Integer codigoUoResponsable,
            Integer codigoEstado,
            Integer vigencia
    ) {

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "iCodigoReq=\"" + numero + "\" " +
                        "vTituloReq=\"" + escaparXml(titulo) + "\" " +
                        "cFechaInicialReq=\"" + escaparXml(fechaInicio) + "\" " +
                        "cFechaFinalReq=\"" + escaparXml(fechaFin) + "\" " +
                        "iCodigoPerSolicitante=\"" + codigoPersonaSolicitante + "\" " +
                        "iCodUoSolicitante=\"" + codigoUoSolicitante + "\" " +
                        "iCodigoPerResponsable=\"" + codigoPersonaResponsable + "\" " +
                        "iCodUoResponsable=\"" + codigoUoResponsable + "\" " +
                        "siCodigoEst=\"" + codigoEstado + "\" " +
                        "siVigencia=\"" + vigencia + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery(
                        "dbo.spGR_Requerimiento_BuscarFiltros"
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

        List<Requerimiento> requerimientos =
                new ArrayList<>();

        for (Object[] fila : resultados) {

            Requerimiento requerimiento =
                    new Requerimiento();

            // 0 - codigoRequerimiento
            requerimiento.setCodigoRequerimiento(
                    convertirInteger(fila[0])
            );

            // 1 - titulo
            requerimiento.setTitulo(
                    convertirString(fila[1])
            );

            // 2 - fechaRegistro
            requerimiento.setFechaRegistro(
                    convertirFecha(fila[2])
            );

            // 3 - descripcion
            requerimiento.setDescripcion(
                    convertirString(fila[3])
            );

            // 4 - codigoPersonaSolicitante
            requerimiento.setCodigoPersonaSolicitante(
                    convertirInteger(fila[4])
            );

            // 5 - solicitante
            requerimiento.setSolicitante(
                    convertirString(fila[5])
            );

            // 6 - codigoUoSolicitante
            requerimiento.setCodigoUoSolicitante(
                    convertirInteger(fila[6])
            );

            // 7 - unidadOrganicaSolicitante
            requerimiento.setUnidadOrganicaSolicitante(
                    convertirString(fila[7])
            );

            // 8 - codigoCargoSolicitante
            requerimiento.setCodigoCargoSolicitante(
                    convertirInteger(fila[8])
            );

            // 9 - cargoSolicitante
            requerimiento.setCargoSolicitante(
                    convertirString(fila[9])
            );

            // 10 - codigoPersonaResponsable
            requerimiento.setCodigoPersonaResponsable(
                    convertirInteger(fila[10])
            );

            // 11 - responsable
            requerimiento.setResponsable(
                    convertirString(fila[11])
            );

            // 12 - codigoUoResponsable
            requerimiento.setCodigoUoResponsable(
                    convertirInteger(fila[12])
            );

            // 13 - unidadOrganicaResponsable
            requerimiento.setUnidadOrganicaResponsable(
                    convertirString(fila[13])
            );

            // 14 - codigoCargoResponsable
            requerimiento.setCodigoCargoResponsable(
                    convertirInteger(fila[14])
            );

            // 15 - cargoResponsable
            requerimiento.setCargoResponsable(
                    convertirString(fila[15])
            );

            // 16 - codigoEstado
            requerimiento.setCodigoEstado(
                    convertirInteger(fila[16])
            );

            // 17 - estado
            requerimiento.setEstado(
                    convertirString(fila[17])
            );

            // 18 - codigoCategoria
            requerimiento.setCodigoCategoria(
                    convertirInteger(fila[18])
            );

            // 19 - categoria
            requerimiento.setCategoria(
                    convertirString(fila[19])
            );

            // 20 - codigoSubcategoria
            requerimiento.setCodigoSubcategoria(
                    convertirInteger(fila[20])
            );

            // 21 - subcategoria
            requerimiento.setSubcategoria(
                    convertirString(fila[21])
            );

            // 22 - codigoPrioridad
            requerimiento.setCodigoPrioridad(
                    convertirInteger(fila[22])
            );

            // 23 - prioridad
            requerimiento.setPrioridad(
                    convertirString(fila[23])
            );

            // 24 - codigoUoRequerimiento
            requerimiento.setCodigoUoRequerimiento(
                    convertirInteger(fila[24])
            );

            // 25 - unidadOrganicaRequerimiento
            requerimiento.setUnidadOrganicaRequerimiento(
                    convertirString(fila[25])
            );

            // 26 - codigoCategoriaSubcategoria
            requerimiento.setCodigoCategoriaSubcategoria(
                    convertirInteger(fila[26])
            );

            requerimientos.add(requerimiento);
        }

        return requerimientos;
    }

    private Integer convertirInteger(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Number numero) {
            return numero.intValue();
        }

        return Integer.valueOf(
                valor.toString()
        );
    }

    private String convertirString(Object valor) {

        if (valor == null) {
            return null;
        }

        return valor.toString().trim();
    }

    private LocalDateTime convertirFecha(Object valor) {

        if (valor == null) {
            return null;
        }

        if (valor instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        if (valor instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        throw new IllegalArgumentException(
                "No se pudo convertir el valor de fecha: "
                        + valor
        );
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

    // ------------------------------------------------------------------
    // Registro y clasificación (spGR_Requerimiento_Registrar / _Consultar)
    // ------------------------------------------------------------------

    private static final Logger LOG = LoggerFactory.getLogger(RequerimientoRepositoryImpl.class);

    /**
     * spGR_Requerimiento_Registrar con siTipBus = 1.
     * iCodigo_Uo es el iCodUO de la unidad (el SP lo convierte a la PK interna).
     * La descripción se envía en el segundo parámetro (@ptDescripcion_Req), no en el XML.
     */
    @Override
    public RequerimientoRegistroResponse registrar(
            RegistrarRequerimientoRequest solicitud,
            String ipTerminal
    ) {
        int solicitante = valor(solicitud.getCodigoPersonaSolicitante());
        String codPerAct = recortar(solicitud.getCodigoPersonaActualizacion(), 4);
        String sumilla = texto(solicitud.getSumilla());
        String desc = texto(solicitud.getDescripcionHtml());
        int uo = valor(solicitud.getUnidadOrganicaId());
        int cat = valor(solicitud.getCategoriaId());
        int subcat = valor(solicitud.getSubCategoriaId());

        if (solicitante <= 0) {
            throw solicitudInvalida("Falta el código GR del solicitante (codigoPersonaSolicitante).");
        }
        if (codPerAct.isEmpty()) {
            throw solicitudInvalida("Falta el código de personal de auditoría (codigoPersonaActualizacion).");
        }
        if (sumilla.isEmpty()) {
            throw solicitudInvalida("El título (sumilla) es obligatorio.");
        }
        if (desc.isEmpty()) {
            throw solicitudInvalida("La descripción es obligatoria.");
        }
        if (uo <= 0) {
            throw solicitudInvalida("Debe indicar la unidad orgánica a la que se solicita.");
        }
        if (cat <= 0 || subcat <= 0) {
            throw solicitudInvalida("Debe indicar la categoría y la subcategoría.");
        }

        String terminal = recortar((ipTerminal != null && !ipTerminal.isBlank()) ? ipTerminal : "WEB", 20);

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siTipBus=\"1\" " +
                        "iCodigo_Uo=\"" + uo + "\" " +
                        "iCodDivRes=\"" + valor(solicitud.getDivisionId()) + "\" " +
                        "siCodigo_Act=\"" + valor(solicitud.getActivoId()) + "\" " +
                        "siCodigo_Cat=\"" + cat + "\" " +
                        "siCodigo_SubCat=\"" + subcat + "\" " +
                        "vSumilla_Req=\"" + escaparXml(sumilla) + "\" " +
                        "iCodigo_Per=\"" + solicitante + "\" " +
                        "vDatCom=\"" + escaparXml(construirDatosComplementarios(solicitud)) + "\" " +
                        "vCorreosCopia=\"" + escaparXml(construirCopias(solicitud)) + "\" " +
                        "vDocumentoAdjunto_Req=\"" + escaparXml(construirNombreAdjunto(solicitud)) + "\" " +
                        "cCodPerActualizacion=\"" + escaparXml(codPerAct) + "\" " +
                        "cNombreTerminal_Req=\"" + escaparXml(terminal) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("dbo.spGR_Requerimiento_Registrar");
        query.registerStoredProcedureParameter("ptXmlGR", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("ptDescripcion_Req", String.class, ParameterMode.IN);
        query.setParameter("ptXmlGR", xml);
        query.setParameter("ptDescripcion_Req", desc);

        List<?> resultados = query.getResultList();
        Integer reqId = null;
        if (!resultados.isEmpty()) {
            Object fila = resultados.get(0);
            Object val = (fila instanceof Object[] arr && arr.length > 0) ? arr[0] : fila;
            if (val != null) {
                reqId = convertirInteger(val.toString().trim());
            }
        }
        if (reqId == null || reqId <= 0) {
            throw new IllegalStateException("spGR_Requerimiento_Registrar no devolvió el código del requerimiento.");
        }

        return new RequerimientoRegistroResponse(reqId, "REQ-" + reqId, LocalDateTime.now().toString());
    }

    /** spGR_Requerimiento_Registrar con siTipBus = 2 (clasificación técnica). */
    @Override
    public boolean clasificar(
            ClasificarRequerimientoRequest solicitud,
            String ipTerminal
    ) {
        int reqId = valor(solicitud.getRequerimientoId());
        int cat = valor(solicitud.getCategoriaId());
        int subcat = valor(solicitud.getSubCategoriaId());
        int pri = valor(solicitud.getPrioridadId());
        String codPerAct = recortar(solicitud.getCodigoPersonaActualizacion(), 4);

        if (reqId <= 0) {
            throw solicitudInvalida("Falta el código del requerimiento a clasificar.");
        }
        if (cat <= 0 || subcat <= 0) {
            throw solicitudInvalida("Debe indicar la categoría y la subcategoría.");
        }
        if (pri <= 0) {
            throw solicitudInvalida("Debe indicar la prioridad.");
        }
        if (codPerAct.isEmpty()) {
            throw solicitudInvalida("Falta el código de personal de auditoría (codigoPersonaActualizacion).");
        }

        String terminal = recortar((ipTerminal != null && !ipTerminal.isBlank()) ? ipTerminal : "WEB", 20);

        String xml =
                "<R>" +
                        "<XmlGR " +
                        "siTipBus=\"2\" " +
                        "iCodigo_Req=\"" + reqId + "\" " +
                        "siCodigo_Cat=\"" + cat + "\" " +
                        "siCodigo_SubCat=\"" + subcat + "\" " +
                        "iCodigo_Pri=\"" + pri + "\" " +
                        "vObservacion_Req=\"" + escaparXml(texto(solicitud.getObservacion())) + "\" " +
                        "cCodPerActualizacion=\"" + escaparXml(codPerAct) + "\" " +
                        "cNombreTerminal_Req=\"" + escaparXml(terminal) + "\" " +
                        "/>" +
                        "</R>";

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("dbo.spGR_Requerimiento_Registrar");
        query.registerStoredProcedureParameter("ptXmlGR", String.class, ParameterMode.IN);
        query.setParameter("ptXmlGR", xml);
        query.execute();
        return true;
    }

    /** spGR_Requerimiento_Consultar con siTipBus = 2 (datos) y 3 (nombres); se completa con datos y adjuntos. */
    @Override
    public RequerimientoDetalleDTO obtenerParaClasificar(Integer id) {
        List<Object[]> filas = ejecutarConsulta("dbo.spGR_Requerimiento_Consultar",
                "<R><XmlGR siTipBus=\"2\" iCodigo_Req=\"" + id + "\" /></R>");
        if (filas.isEmpty()) {
            return null;
        }

        Object[] fila = filas.get(0);
        RequerimientoDetalleDTO dto = new RequerimientoDetalleDTO();
        dto.setRequerimientoId(entero(fila, 0) != null ? entero(fila, 0) : id);
        dto.setNumeroRequerimiento("REQ-" + dto.getRequerimientoId());
        dto.setFechaRequerimiento(texto(fila, 1));
        String descripcion = texto(fila, 2) == null ? "" : texto(fila, 2);
        dto.setDescripcionHtml(descripcion);
        dto.setUnidadOrganicaDestinoId(entero(fila, 4));
        dto.setCodigoEstado(entero(fila, 7));
        dto.setEstadoActual(texto(fila, 8));
        dto.setActivoId(entero(fila, 10));
        dto.setCategoriaId(entero(fila, 11));
        dto.setSubCategoriaId(entero(fila, 12));
        dto.setPrioridadId(entero(fila, 13));
        dto.setObservacion(texto(fila, 20) == null ? "" : texto(fila, 20));
        dto.setUnidadOrganicaSolicitante(texto(fila, 31) == null ? "" : texto(fila, 31));
        dto.setSolicitanteNombreCompleto(texto(fila, 32) == null ? "" : texto(fila, 32));
        dto.setCargoSolicitante(texto(fila, 33) == null ? "" : texto(fila, 33));
        dto.setSumilla(tituloDesde(descripcion));

        enriquecerDetalle(dto, id);
        return dto;
    }

    /** Cada bloque es independiente: si uno falla se registra y se devuelve el detalle básico. */
    private void enriquecerDetalle(RequerimientoDetalleDTO dto, Integer id) {
        try {
            List<Object[]> filas = ejecutarConsulta("dbo.spGR_Requerimiento_Consultar",
                    "<R><XmlGR siTipBus=\"3\" iCodigo_Req=\"" + id + "\" /></R>");
            if (!filas.isEmpty()) {
                Object[] f = filas.get(0);
                // 5 Prioridad, 20 Ubicación (UO destino), 27 vNombre_Cat, 30 vNombre_SubCat, 46 vSumilla_Req
                dto.setPrioridadNombre(texto(f, 5));
                dto.setUnidadOrganicaDestinoNombre(texto(f, 20));
                dto.setCategoriaNombre(texto(f, 27));
                dto.setSubCategoriaNombre(texto(f, 30));
                String sumilla = texto(f, 46);
                if (sumilla != null && !sumilla.isBlank()) {
                    dto.setSumilla(sumilla);
                }
            }
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron leer los nombres del requerimiento {}: {}", id, e.getMessage());
        }

        try {
            List<Map<String, Object>> datos = new ArrayList<>();
            for (Object[] f : ejecutarConsulta("dbo.spGR_Requerimiento_ConsultarDatosComplementarios",
                    "<R><XmlGR siTipBus=\"1\" iCodigo_Req=\"" + id + "\"/></R>")) {
                datos.add(Map.of(
                        "tipoDatoDescripcion", String.valueOf(texto(f, 1)),
                        "valor", String.valueOf(texto(f, 2))));
            }
            dto.setDatosComplementarios(datos);
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron leer los datos complementarios del requerimiento {}: {}", id, e.getMessage());
        }

        try {
            List<Map<String, Object>> adjuntos = new ArrayList<>();
            for (Object[] f : ejecutarConsulta("dbo.spGR_Requerimiento_ConsultarDocumentoAdjunto",
                    "<R><XmlGR siTipBus=\"1\" iCodigo_Req=\"" + id + "\"/></R>")) {
                String original = texto(f, 5);
                adjuntos.add(Map.of(
                        "nombreArchivo", original != null && !original.isBlank() ? original : String.valueOf(texto(f, 4)),
                        "descripcion", texto(f, 6) == null ? "" : texto(f, 6)));
            }
            dto.setDocumentosAdjuntos(adjuntos);
        } catch (RuntimeException e) {
            LOG.warn("No se pudieron leer los adjuntos del requerimiento {}: {}", id, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> ejecutarConsulta(String sp, String xml) {
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery(sp);
        query.registerStoredProcedureParameter("ptXmlGR", String.class, ParameterMode.IN);
        query.setParameter("ptXmlGR", xml);
        List<Object[]> salida = new ArrayList<>();
        for (Object f : query.getResultList()) {
            salida.add(f instanceof Object[] arr ? arr : new Object[]{f});
        }
        return salida;
    }

    /** vCorreosCopia = cCodPer separados por coma; el SP arma SQL dinámico, por eso solo 1 a 4 dígitos. */
    private String construirCopias(RegistrarRequerimientoRequest solicitud) {
        if (solicitud.getPersonasCopiaCodigos() == null) {
            return "";
        }
        return solicitud.getPersonasCopiaCodigos().stream()
                .filter(c -> c != null && c.trim().matches("\\d{1,4}"))
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining(","));
    }

    /** Nombre base "FileReqN{ext}": el SP intercala el código del requerimiento después del 8vo carácter. */
    private String construirNombreAdjunto(RegistrarRequerimientoRequest solicitud) {
        if (solicitud.getArchivosAdjuntos() == null || solicitud.getArchivosAdjuntos().isEmpty()) {
            return "";
        }
        Object original = solicitud.getArchivosAdjuntos().get(0).get("nombreOriginal");
        return "FileReqN" + DocumentoStorage.extension(original == null ? null : original.toString());
    }

    /** vDatCom = "codigoTipoDato,valor;codigoTipoDato,valor"; se retiran ';' y ',' de los valores. */
    private String construirDatosComplementarios(RegistrarRequerimientoRequest solicitud) {
        if (solicitud.getDatosComplementarios() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> dato : solicitud.getDatosComplementarios()) {
            Object tipo = dato.get("tipoDatoId");
            Object valor = dato.get("valor");
            if (tipo == null || valor == null || valor.toString().isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(tipo).append(',').append(valor.toString().replace(";", " ").replace(",", " ").trim());
        }
        return sb.length() > 4000 ? sb.substring(0, 4000) : sb.toString();
    }

    /** El SP no devuelve título: se usa el texto de la descripción sin HTML (máx. 80 caracteres). */
    private String tituloDesde(String html) {
        if (html == null) {
            return null;
        }
        String texto = html.replaceAll("<[^>]*>", " ").replace("&nbsp;", " ").replaceAll("\\s+", " ").trim();
        return texto.length() > 80 ? texto.substring(0, 80) + "..." : texto;
    }

    private ResponseStatusException solicitudInvalida(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }

    private int valor(Integer numero) {
        return numero == null ? 0 : numero;
    }

    private String texto(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private String recortar(String valor, int maximo) {
        String limpio = texto(valor);
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }

    private String texto(Object[] fila, int indice) {
        return fila.length > indice && fila[indice] != null ? fila[indice].toString().trim() : null;
    }

    private Integer entero(Object[] fila, int indice) {
        if (fila.length <= indice || fila[indice] == null) {
            return null;
        }
        Object v = fila[indice];
        if (v instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.valueOf(v.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
