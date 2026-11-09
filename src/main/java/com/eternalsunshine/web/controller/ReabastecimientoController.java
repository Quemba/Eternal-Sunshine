package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.ReabastecimientoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReabastecimientoController {

    @Autowired
    private ReabastecimientoService reabastecimientoService;

    @GetMapping("/reabastecimiento")
    public String ver(Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<ReabastecimientoService.Sugerencia> sugerencias = reabastecimientoService.calcular();

        long urgentes = sugerencias.stream()
                .filter(ReabastecimientoService.Sugerencia::isUrgente)
                .count();

        model.addAttribute("sugerencias", sugerencias);
        model.addAttribute("urgentes", urgentes);
        model.addAttribute("pronto", sugerencias.size() - urgentes);
        model.addAttribute("ventanaDias", ReabastecimientoService.VENTANA_DIAS);
        model.addAttribute("coberturaDias", ReabastecimientoService.COBERTURA_DIAS);
        model.addAttribute("alertaDias", ReabastecimientoService.ALERTA_DIAS);
        model.addAttribute("esAdmin", Rol.ADMIN.equals(usuario.getRol()));

        return "reabastecimiento";
    }
}