package com.eternalsunshine.web.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Permissions-Policy", "camera=(self), microphone=(), geolocation=(), payment=()");
        response.setHeader("Content-Security-Policy", "frame-ancestors 'none'; base-uri 'self'; object-src 'none'");

        if (request.isSecure()) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000");
        }

        if (!esEstatico(request)) {
            response.setHeader("Cache-Control", "no-store");
        }

        chain.doFilter(request, response);
    }

    private boolean esEstatico(HttpServletRequest request) {

        String ruta = request.getRequestURI();

        return ruta.startsWith("/css/")
                || ruta.startsWith("/js/")
                || ruta.startsWith("/images/");
    }
}