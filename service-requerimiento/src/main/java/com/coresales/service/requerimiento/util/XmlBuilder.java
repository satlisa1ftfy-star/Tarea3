package com.coresales.service.requerimiento.util;
import org.apache.commons.text.StringEscapeUtils;
import java.util.Map;

public final class XmlBuilder {

    private XmlBuilder() {
    }
    public static String elemento(
            String nombre,
            Map<String, Object> atributos
    ) {
        StringBuilder xml = new StringBuilder();
        xml.append("<").append(nombre);
        atributos.forEach((clave, valor) -> {
            // Nulo: no se envía el atributo; OPENXML lo lee como NULL.
            if (valor == null) return;
            xml.append(" ")
                    .append(clave)
                    .append("=\"")
                    .append(StringEscapeUtils.escapeXml10(
                            valor.toString()
                    ))
                    .append("\"");
        });
        xml.append("/>");
        return xml.toString();
    }
}