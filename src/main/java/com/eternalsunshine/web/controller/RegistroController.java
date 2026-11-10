package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.config.RegistroLimiteService;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.TiendaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegistroController {

    @Autowired
    private TiendaService tiendaService;

    @Autowired
    private RegistroLimiteService limite;

    @GetMapping("/registro")
    public String mostrar(HttpSession session, Model model) {

        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/";
        }

        model.addAttribute("registroAbierto", tiendaService.registroAbierto());

        return "registro";
    }

    @GetMapping("/terminos")
    public String terminos() {
        return "terminos";
    }

    @PostMapping("/registro")
    public String procesar(
            @RequestParam(defaultValue = "") String tienda,
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String confirmacion,
            @RequestParam(defaultValue = "") String acepto,
            @RequestParam(name = "sitio_web", defaultValue = "") String trampa,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        model.addAttribute("registroAbierto", tiendaService.registroAbierto());
        model.addAttribute("tienda", tienda);
        model.addAttribute("username", username);

        String ip = request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();

        if (!limite.permitir(ip)) {
            response.setStatus(429);
            model.addAttribute("error", "Demasiados intentos desde esta conexión. Inténtalo de nuevo en una hora.");
            return "registro";
        }

        if (!trampa.isEmpty()) {
            model.addAttribute("error", "No se pudo completar el registro.");
            return "registro";
        }

        if (acepto.isEmpty()) {
            model.addAttribute("error", "Debes aceptar los términos y el tratamiento de datos para continuar.");
            return "registro";
        }

        if (!password.trim().equals(confirmacion.trim())) {
            model.addAttribute("error", "La contraseña y su confirmación no coinciden.");
            return "registro";
        }

        try {
            Usuario admin = tiendaService.registrar(tienda, username, password);

            HttpSession sesion = request.getSession(true);
            request.changeSessionId();
            sesion.setAttribute("usuarioLogueado", admin);
            sesion.setAttribute("tiendaNombre", tiendaService.nombreDe(admin.getTiendaId()));

            return "redirect:/";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());

        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error", "Ese nombre de usuario ya está en uso. Elige otro.");
        }

        return "registro";
    }
}
