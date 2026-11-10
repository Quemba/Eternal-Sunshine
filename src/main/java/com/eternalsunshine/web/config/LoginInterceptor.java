package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final String ATRIBUTO_VERIFICADO = "usuarioVerificadoEn";
    private static final long REVALIDAR_CADA_MS = 60_000L;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        Usuario usuario = (session != null)
                ? (Usuario) session.getAttribute("usuarioLogueado")
                : null;

        if (usuario == null) {
            response.sendRedirect("/login");
            return false;
        }

        long ahora = System.currentTimeMillis();
        Object ultima = session.getAttribute(ATRIBUTO_VERIFICADO);

        if (!(ultima instanceof Long) || ahora - (Long) ultima > REVALIDAR_CADA_MS) {

            Usuario actual = usuario.getId() == null
                    ? null
                    : usuarioRepository.findById(usuario.getId()).orElse(null);

            boolean valido = actual != null
                    && actual.estaActivo()
                    && Objects.equals(actual.getPassword(), usuario.getPassword())
                    && actual.getTiendaId() != null
                    && Objects.equals(actual.getTiendaId(), usuario.getTiendaId());

            if (!valido) {
                session.invalidate();
                response.sendRedirect("/login");
                return false;
            }

            session.setAttribute("usuarioLogueado", actual);
            session.setAttribute(ATRIBUTO_VERIFICADO, ahora);
        }

        return true;
    }
}