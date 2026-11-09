package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import com.eternalsunshine.web.service.HistorialConsultaService;
import com.eternalsunshine.web.service.TipoAccionHistorial;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class HistorialController {

    @Autowired
    private HistorialConsultaService consultaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private LocalDate leerFecha(String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/historial")
    public String mostrarHistorial(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer usuario,
            @RequestParam(defaultValue = "") String tipo,
            @RequestParam(defaultValue = "") String desde,
            @RequestParam(defaultValue = "") String hasta,
            @RequestParam(defaultValue = "1") int pagina,
            Model model,
            HttpSession session) {

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");
        boolean admin = usuarioActual != null && Rol.ADMIN.equals(usuarioActual.getRol());

        Integer usuarioFiltro = admin
                ? usuario
                : (usuarioActual == null ? Integer.valueOf(-1) : usuarioActual.getId());

        HistorialConsultaService.Resultado resultado = consultaService.buscar(
                q, usuarioFiltro, tipo, leerFecha(desde), leerFecha(hasta), pagina);

        List<TipoAccionHistorial> tipos = new ArrayList<>();

        for (TipoAccionHistorial t : TipoAccionHistorial.values()) {
            if (t != TipoAccionHistorial.OTRO) {
                tipos.add(t);
            }
        }

        List<Usuario> usuarios = admin
                ? usuarioRepository.findAll(Sort.by("username"))
                : (usuarioActual == null ? new ArrayList<>() : List.of(usuarioActual));

        model.addAttribute("resultado", resultado);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("tipos", tipos);

        model.addAttribute("q", q);
        model.addAttribute("usuarioId", usuarioFiltro);
        model.addAttribute("usuarioTexto", usuarioFiltro == null ? "" : String.valueOf(usuarioFiltro));
        model.addAttribute("tipo", tipo);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        boolean hayFiltros = !q.trim().isEmpty() || (admin && usuario != null)
                || !tipo.isEmpty() || !desde.isEmpty() || !hasta.isEmpty();
        model.addAttribute("hayFiltros", hayFiltros);

        model.addAttribute("esAdmin", admin);

        return "historial";
    }
}