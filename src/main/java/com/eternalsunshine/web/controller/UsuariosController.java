package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/usuarios")
    public String mostrarUsuarios(Model model) {

        model.addAttribute(
                "usuarios",
                usuarioService.obtenerUsuarios());

        return "usuarios";
    }
}