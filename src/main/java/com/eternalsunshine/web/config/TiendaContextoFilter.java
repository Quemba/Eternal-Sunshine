package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Fija la tienda de la persona con sesión antes de que se abra cualquier conexión a la base.
 * Debe ejecutarse antes que el resto de la petición.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TiendaContextoFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);

            Object atributo = session == null ? null : session.getAttribute("usuarioLogueado");

            if (atributo instanceof Usuario usuario && usuario.getTiendaId() != null) {
                TiendaContexto.fijar(usuario.getTiendaId());
            } else {
                TiendaContexto.limpiar();
            }

            chain.doFilter(request, response);

        } finally {
            TiendaContexto.limpiar();
        }
    }
}
