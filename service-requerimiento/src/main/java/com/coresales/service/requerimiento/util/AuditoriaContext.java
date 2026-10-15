package com.coresales.service.requerimiento.util;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Getter
@Component
@RequestScope
public class AuditoriaContext {

    private final HttpServletRequest request;

    public AuditoriaContext(HttpServletRequest request) {
        this.request = request;
    }

    public String getIpCliente() {
        return HttpRequestUtil.obtenerIpCliente(request);
    }
}