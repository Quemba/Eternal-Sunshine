package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.ProductoService;
import com.eternalsunshine.web.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProductoService productoService;

    @GetMapping("/login")
    public String mostrarLogin() {

        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(
            @RequestParam String username,
            @RequestParam String password,
            Model model) {

        Usuario usuario =
                usuarioService.login(
                        username,
                        password);

        if (usuario != null) {

            model.addAttribute(
                    "usuario",
                    usuario.getUsername());

            model.addAttribute(
                    "totalProductos",
                    productoService.obtenerTotalProductos());

            model.addAttribute(
                    "stockBajo",
                    productoService.obtenerStockBajo());

            model.addAttribute(
                    "agotados",
                    productoService.obtenerAgotados());

            model.addAttribute(
                    "valorInventario",
                    productoService.obtenerValorInventario());

            return "dashboard";
        }

        model.addAttribute(
                "error",
                "Usuario o contraseña incorrectos");

        return "login";
    }
}