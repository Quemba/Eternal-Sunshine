package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import com.eternalsunshine.web.service.HistorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class UsuarioFormController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistorialService historialService;

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "usuario",
                new Usuario());

        return "usuario-form";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuario(
            @PathVariable Integer id,
            Model model) {

        Usuario usuario =
                usuarioRepository
                        .findById(id)
                        .orElse(null);

        model.addAttribute(
                "usuario",
                usuario);

        return "usuario-form";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(
            @ModelAttribute Usuario usuario) {

        boolean nuevo =
                (usuario.getId() == null);

        usuarioRepository.save(usuario);

        if (nuevo) {

            historialService.registrar(
                    "admin",
                    "CREÓ USUARIO: "
                            + usuario.getUsername());

        } else {

            historialService.registrar(
                    "admin",
                    "EDITÓ USUARIO: "
                            + usuario.getUsername());
        }

        return "redirect:/usuarios";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(
            @PathVariable Integer id) {

        Usuario usuario =
                usuarioRepository
                        .findById(id)
                        .orElse(null);

        if (usuario != null) {

            historialService.registrar(
                    "admin",
                    "ELIMINÓ USUARIO: "
                            + usuario.getUsername());
        }

        usuarioRepository.deleteById(id);

        return "redirect:/usuarios";
    }
}