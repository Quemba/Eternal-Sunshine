package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.config.RecuperacionLimiteService;
import com.eternalsunshine.web.config.TiendaContexto;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.HistorialService;
import com.eternalsunshine.web.service.RecuperacionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Controller
public class RecuperacionController {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionController.class);

    @Value("${app.url:}")
    private String urlPublica;

    @Autowired
    private RecuperacionService recuperacionService;

    @Autowired
    private RecuperacionLimiteService limite;

    @Autowired
    private HistorialService historialService;

    private String urlBase(HttpServletRequest request) {

        if (urlPublica != null && !urlPublica.isBlank()) {
            return urlPublica.trim().replaceAll("/+$", "");
        }

        return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
    }

    @GetMapping("/recuperar")
    public String mostrar(HttpSession session) {

        if (session.getAttribute("usuarioLogueado") != null) {
            return "redirect:/cuenta/contrasena";
        }

        return "recuperar";
    }

    @PostMapping("/recuperar")
    public String solicitar(
            @RequestParam(defaultValue = "") String dato,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        String ip = request.getRemoteAddr() == null ? "desconocida" : request.getRemoteAddr();

        if (!limite.permitir(ip, dato)) {
            response.setStatus(429);
            model.addAttribute("error", "Demasiadas solicitudes. Inténtalo de nuevo en una hora.");
            return "recuperar";
        }

        try {
            recuperacionService.solicitar(dato, urlBase(request));

        } catch (Exception e) {
            log.error("No se pudo procesar la recuperación: {}", e.getMessage());
        }

        model.addAttribute("enviado", true);

        return "recuperar";
    }

    @GetMapping("/restablecer")
    public String formulario(
            @RequestParam(defaultValue = "") String token,
            Model model) {

        model.addAttribute("token", token);
        model.addAttribute("valido", recuperacionService.tokenValido(token));

        return "restablecer";
    }

    @PostMapping("/restablecer")
    public String restablecer(
            @RequestParam(defaultValue = "") String token,
            @RequestParam(defaultValue = "") String nueva,
            @RequestParam(defaultValue = "") String confirmacion,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            Usuario usuario = recuperacionService.restablecer(token, nueva, confirmacion);

            try {
                TiendaContexto.ejecutar(usuario.getTiendaId(), () ->
                        historialService.registrar(usuario,
                                "EDITÓ USUARIO: " + usuario.getUsername()
                                        + " — restableció su contraseña con el enlace del correo"));
            } catch (Exception e) {
                log.warn("No se pudo dejar constancia en el historial: {}", e.getMessage());
            }

            redirectAttributes.addFlashAttribute("mensaje",
                    "Contraseña nueva guardada. Ya puedes iniciar sesión.");

            return "redirect:/login";

        } catch (RecuperacionService.TokenInvalidoException e) {
            model.addAttribute("token", "");
            model.addAttribute("valido", false);

        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("valido", true);
            model.addAttribute("error", e.getMessage());
        }

        return "restablecer";
    }
}
