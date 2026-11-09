package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        Usuario usuario = session == null
                ? null
                : (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario != null && Rol.ADMIN.equals(usuario.getRol())) {
            return true;
        }

        response.sendRedirect(usuario == null ? "/login" : "/");

        return false;
    }
}