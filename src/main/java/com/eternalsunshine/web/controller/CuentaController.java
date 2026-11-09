package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.config.LoginIntentosService;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CuentaController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private LoginIntentosService intentos;

    private Usuario sesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    @GetMapping("/cuenta/contrasena")
    public String mostrar(Model model, HttpSession session) {

        Usuario usuario = sesion(session);

        model.addAttribute("esAdmin", usuario != null && Rol.ADMIN.equals(usuario.getRol()));

        return "cuenta-contrasena";
    }

    @PostMapping("/cuenta/contrasena")
    public String cambiar(
            @RequestParam(defaultValue = "") String actual,
            @RequestParam(defaultValue = "") String nueva,
            @RequestParam(defaultValue = "") String confirmacion,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = sesion(session);

        if (usuario == null) {
            return "redirect:/login";
        }

        String ip = request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();

        long espera = intentos.milisegundosBloqueado(ip, usuario.getUsername());

        if (espera > 0) {

            long minutos = (espera + 59_999) / 60_000;

            redirectAttributes.addFlashAttribute("error",
                    "Demasiados intentos fallidos. Inténtalo de nuevo en "
                            + minutos + (minutos == 1 ? " minuto." : " minutos."));

            return "redirect:/cuenta/contrasena";
        }

        try {
            Usuario actualizado = usuarioService.cambiarMiPassword(
                    usuario.getId(), actual, nueva, confirmacion);

            intentos.registrarExito(ip, usuario.getUsername());

            session.setAttribute("usuarioLogueado", actualizado);
            request.changeSessionId();

            redirectAttributes.addFlashAttribute("mensaje", "Tu contraseña se cambió correctamente.");

        } catch (UsuarioService.PasswordActualIncorrectaException e) {
            intentos.registrarFallo(ip, usuario.getUsername());
            redirectAttributes.addFlashAttribute("error", e.getMessage());

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cuenta/contrasena";
    }
}