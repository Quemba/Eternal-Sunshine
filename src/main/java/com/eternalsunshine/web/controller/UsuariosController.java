package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class UsuariosController {

    @Autowired
    private UsuarioService usuarioService;

    private Usuario sesion(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = sesion(session);
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    @GetMapping("/usuarios")
    public String mostrarUsuarios(Model model, HttpSession session) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        List<Usuario> usuarios = new ArrayList<>(usuarioService.obtenerUsuarios());

        usuarios.sort(Comparator.comparing((Usuario u) ->
                u.getUsername() == null ? "" : u.getUsername().toLowerCase(Locale.ROOT)));

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", usuarioService.opcionesRol());
        model.addAttribute("miId", sesion(session).getId());
        model.addAttribute("esAdmin", true);

        return "usuarios";
    }

    @PostMapping("/usuarios/crear")
    public String crear(
            @RequestParam(defaultValue = "") String username,
            @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String rol,
            @RequestParam(defaultValue = "") String tienda,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        try {
            Usuario creado = usuarioService.crear(username, password, rol, tienda, sesion(session));

            redirectAttributes.addFlashAttribute("mensaje",
                    "Usuario «" + creado.getUsername() + "» creado como "
                            + UsuarioService.etiquetaRol(creado.getRol()).toLowerCase(Locale.ROOT) + ".");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("formUsername", username);
            redirectAttributes.addFlashAttribute("formRol", rol);
            redirectAttributes.addFlashAttribute("formTienda", tienda);

        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un usuario con ese nombre.");
            redirectAttributes.addFlashAttribute("formUsername", username);
            redirectAttributes.addFlashAttribute("formRol", rol);
            redirectAttributes.addFlashAttribute("formTienda", tienda);
        }

        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String rol,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        try {
            usuarioService.cambiarRol(id, rol, sesion(session));

            redirectAttributes.addFlashAttribute("mensaje",
                    "Rol actualizado. Si esa persona tiene la sesión abierta, se aplica en menos de un minuto.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/password")
    public String restablecerPassword(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        try {
            usuarioService.restablecerPassword(id, password, sesion(session));

            redirectAttributes.addFlashAttribute("mensaje",
                    "Contraseña restablecida. Entrégasela a esa persona en privado. Su sesión abierta, si tiene una, se cerrará en menos de un minuto.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/usuarios";
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(
            @PathVariable Integer id,
            @RequestParam boolean activo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        try {
            usuarioService.cambiarEstado(id, activo, sesion(session));

            redirectAttributes.addFlashAttribute("mensaje",
                    activo
                            ? "Usuario reactivado. Ya puede iniciar sesión."
                            : "Usuario desactivado. Ya no puede iniciar sesión y su sesión abierta se cerrará en menos de un minuto.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/usuarios";
    }
}