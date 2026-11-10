package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.AvisosService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Locale;

@Component
public class AvisosInterceptor implements HandlerInterceptor {

    public static class SesionVista {

        private final String nombre;
        private final String rol;
        private final String inicial;
        private final String tienda;

        public SesionVista(String nombre, String rol, String inicial, String tienda) {
            this.nombre = nombre;
            this.rol = rol;
            this.inicial = inicial;
            this.tienda = tienda;
        }

        public String getTienda() { return tienda; }

        public String getNombre() { return nombre; }
        public String getRol() { return rol; }
        public String getInicial() { return inicial; }
    }

    @Autowired
    private AvisosService avisosService;

    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) {

        if (modelAndView == null || !modelAndView.hasView()) {
            return;
        }

        String vista = modelAndView.getViewName();

        if (vista == null || vista.startsWith("redirect:") || vista.startsWith("forward:")) {
            return;
        }

        HttpSession session = request.getSession(false);

        Usuario usuario = session == null
                ? null
                : (Usuario) session.getAttribute("usuarioLogueado");

        if (!modelAndView.getModel().containsKey("avisos")) {
            modelAndView.addObject("avisos", avisosService.para(usuario));
        }

        if (usuario != null) {

            String nombre = usuario.getUsername() == null ? "" : usuario.getUsername();
            String inicial = nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase(Locale.ROOT);
            String rol = Rol.ADMIN.equals(usuario.getRol()) ? "Administrador" : "Empleado";

            Object nombreTienda = session.getAttribute("tiendaNombre");

            modelAndView.addObject("sesionUsuario", new SesionVista(
                    nombre, rol, inicial, nombreTienda == null ? "" : nombreTienda.toString()));
        }
    }
}