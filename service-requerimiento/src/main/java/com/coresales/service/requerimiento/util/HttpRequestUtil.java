package com.coresales.service.requerimiento.util;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class HttpRequestUtil {

    private HttpRequestUtil() {}
    /**
     * Obtiene la IP del cliente considerando X-Forwarded-For
     * cuando la petición pasa por un proxy o balanceador.
     */
    public static String obtenerIpCliente(HttpServletRequest request) {

        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            int separador = forwardedFor.indexOf(',');

            return separador >= 0
                    ? forwardedFor.substring(0, separador).trim()
                    : forwardedFor.trim();
        }

        return request.getRemoteAddr();
    }

}