package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AcercaController {

    @GetMapping("/acerca")
    public String mostrar(Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        model.addAttribute("esAdmin", usuario != null && Rol.ADMIN.equals(usuario.getRol()));

        return "acerca";
    }
}