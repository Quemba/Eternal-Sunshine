package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.AvisosService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AvisosController {

    @Autowired
    private AvisosService avisosService;

    @GetMapping("/avisos")
    @ResponseBody
    public AvisosService.Avisos avisos(HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        return avisosService.para(usuario);
    }
}