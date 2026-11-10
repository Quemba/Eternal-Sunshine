package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.config.LoginIntentosService;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.TiendaService;
import com.eternalsunshine.web.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private TiendaService tiendaService;

    @Autowired
    private LoginIntentosService intentos;

    @GetMapping("/login")
    public String mostrarLogin(
            @RequestParam(required = false) String salio,
            Model model) {

        if (salio != null) {
            model.addAttribute("mensaje", "Sesión cerrada. Esto tampoco lo recordaremos.");
        }

        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String ip = request.getRemoteAddr();

        if (ip == null) {
            ip = "desconocida";
        }

        long espera = intentos.milisegundosBloqueado(ip, username);

        if (espera > 0) {

            long minutos = (espera + 59_999) / 60_000;

            model.addAttribute(
                    "error",
                    "Demasiados intentos fallidos. Inténtalo de nuevo en "
                            + minutos + (minutos == 1 ? " minuto." : " minutos."));

            response.setStatus(429);

            return "login";
        }

        Usuario usuario =
                usuarioService.login(
                        username,
                        password);

        if (usuario != null) {

            intentos.registrarExito(ip, username);

            HttpSession sesion = request.getSession(true);
            request.changeSessionId();
            sesion.setAttribute("usuarioLogueado", usuario);
            sesion.setAttribute("tiendaNombre", tiendaService.nombreDe(usuario.getTiendaId()));

            return "redirect:/";
        }

        intentos.registrarFallo(ip, username);

        model.addAttribute(
                "error",
                "Usuario o contraseña incorrectos");

        return "login";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?salio";
    }
}