package com.eternalsunshine.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;

public class CsrfOrigenInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (esMetodoSeguro(request.getMethod())) {
            return true;
        }

        if (vieneDelMismoOrigen(request)) {
            return true;
        }

        response.sendError(
                HttpServletResponse.SC_FORBIDDEN,
                "Solicitud bloqueada por seguridad: el origen no coincide.");

        return false;
    }

    private boolean esMetodoSeguro(String metodo) {
        return "GET".equalsIgnoreCase(metodo)
                || "HEAD".equalsIgnoreCase(metodo)
                || "OPTIONS".equalsIgnoreCase(metodo)
                || "TRACE".equalsIgnoreCase(metodo);
    }

    private boolean vieneDelMismoOrigen(HttpServletRequest request) {

        String sitio = request.getHeader("Sec-Fetch-Site");

        if (sitio != null && !sitio.isEmpty()) {
            return "same-origin".equalsIgnoreCase(sitio)
                    || "none".equalsIgnoreCase(sitio);
        }

        String origen = request.getHeader("Origin");

        if (origen != null && !origen.isEmpty()) {
            return coincideConHost(origen, request);
        }

        String referer = request.getHeader("Referer");

        if (referer != null && !referer.isEmpty()) {
            return coincideConHost(referer, request);
        }

        return true;
    }

    private boolean coincideConHost(String url, HttpServletRequest request) {
        try {
            String autoridad = new URI(url).getRawAuthority();
            String host = request.getHeader("Host");

            return autoridad != null
                    && host != null
                    && autoridad.equalsIgnoreCase(host);

        } catch (Exception e) {
            return false;
        }
    }
}