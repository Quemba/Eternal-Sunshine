package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.HistorialRepository;
import com.eternalsunshine.web.repository.UsuarioRepository;
import com.eternalsunshine.web.service.HistorialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UsuarioFormController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private HistorialRepository historialRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    private boolean esPasswordSegura(String password) {
        return password.length() >= 8
                && password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*");
    }

    @GetMapping("/usuarios/nuevo")
    public String mostrarFormulario(Model model, HttpSession session) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        model.addAttribute("usuario", new Usuario());
        return "usuario-form";
    }

    @GetMapping("/usuarios/editar/{id}")
    public String editarUsuario(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            return "redirect:/usuarios";
        }

        model.addAttribute("usuario", usuario);
        return "usuario-form";
    }

    @PostMapping("/usuarios/guardar")
    public String guardarUsuario(
            @ModelAttribute Usuario usuario,
            HttpSession session,
            Model model) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        boolean nuevo = (usuario.getId() == null);

        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()
                || usuario.getRol() == null
                || (nuevo && (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()))) {

            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "Usuario y rol son obligatorios (la contraseña también, al crear un usuario nuevo).");
            return "usuario-form";
        }

        if (usuario.getUsername().length() > 50
                || (usuario.getTienda() != null && usuario.getTienda().length() > 100)) {

            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "El nombre de usuario no puede superar 50 caracteres, ni la tienda 100.");
            return "usuario-form";
        }

        if (usuario.getPassword() != null && !usuario.getPassword().trim().isEmpty()
                && !esPasswordSegura(usuario.getPassword().trim())) {

            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "La contraseña debe tener al menos 8 caracteres, con al menos una mayúscula, una minúscula y un número.");
            return "usuario-form";
        }

        usuario.setUsername(usuario.getUsername().trim());
        if (usuario.getTienda() != null) {
            usuario.setTienda(usuario.getTienda().trim());
        }

        Usuario existente = usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername());
        if (existente != null && (nuevo || !existente.getId().equals(usuario.getId()))) {
            model.addAttribute("usuario", usuario);
            model.addAttribute("error", "Ya existe un usuario con ese nombre.");
            return "usuario-form";
        }

        if (!nuevo) {
            Usuario actual = usuarioRepository.findById(usuario.getId()).orElse(null);

            if (actual == null) {
                model.addAttribute("error", "Ese usuario ya no existe.");
                return "usuario-form";
            }

            if (Rol.ADMIN.equals(actual.getRol())
                    && !Rol.ADMIN.equals(usuario.getRol())
                    && usuarioRepository.countByRol(Rol.ADMIN) <= 1) {

                model.addAttribute("usuario", usuario);
                model.addAttribute("error", "No puedes quitarle el rol ADMIN al último administrador del sistema.");
                return "usuario-form";
            }

            if (usuario.getPassword() == null || usuario.getPassword().trim().isEmpty()) {
                usuario.setPassword(actual.getPassword());
            }
        }

        String password = usuario.getPassword();
        if (password != null && !password.startsWith("$2")) {
            usuario.setPassword(passwordEncoder.encode(password));
        }

        usuarioRepository.save(usuario);

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");

        if (nuevo) {
            historialService.registrar(usuarioActual, "CREÓ USUARIO: " + usuario.getUsername());
        } else {
            historialService.registrar(usuarioActual, "EDITÓ USUARIO: " + usuario.getUsername());
        }

        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");

        if (usuarioActual.getId().equals(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propio usuario mientras tienes la sesión activa.");
            return "redirect:/usuarios";
        }

        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario == null) {
            redirectAttributes.addFlashAttribute("error", "Ese usuario ya no existe.");
            return "redirect:/usuarios";
        }

        if (Rol.ADMIN.equals(usuario.getRol()) && usuarioRepository.countByRol(Rol.ADMIN) <= 1) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar al último administrador del sistema.");
            return "redirect:/usuarios";
        }

        if (historialRepository.existsByUsuario_Id(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar este usuario porque tiene movimientos registrados en el historial.");
            return "redirect:/usuarios";
        }

        historialService.registrar(usuarioActual, "ELIMINÓ USUARIO: " + usuario.getUsername());
        usuarioRepository.deleteById(id);
        return "redirect:/usuarios";
    }
}