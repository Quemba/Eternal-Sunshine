package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.*;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.SolicitudRepository;
import com.eternalsunshine.web.service.AvisosService;
import com.eternalsunshine.web.service.CategoriaService;
import com.eternalsunshine.web.service.HistorialService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
public class SolicitudController {

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private AvisosService avisosService;

    @Autowired
    private CategoriaService categoriaService;

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    @GetMapping("/solicitudes")
    public String listarPendientes(Model model, HttpSession session) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        model.addAttribute("solicitudes", solicitudRepository.findByEstadoOrderByFechaSolicitudDesc(EstadoSolicitud.PENDIENTE));
        model.addAttribute("esAdmin", true);
        return "solicitudes";
    }

    @GetMapping("/solicitudes/mias")
    public String misSolicitudes(Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        List<Solicitud> lista =
                solicitudRepository.findByUsuarioSolicitante_IdOrderByFechaSolicitudDesc(usuario.getId());

        Set<Integer> nuevasIds = new HashSet<>();
        List<Solicitud> porMarcar = new ArrayList<>();

        for (Solicitud s : lista) {
            if (s.getEstado() != EstadoSolicitud.PENDIENTE
                    && Boolean.FALSE.equals(s.getVistaPorSolicitante())) {
                nuevasIds.add(s.getId());
                s.setVistaPorSolicitante(true);
                porMarcar.add(s);
            }
        }

        if (!porMarcar.isEmpty()) {
            solicitudRepository.saveAll(porMarcar);
        }

        model.addAttribute("solicitudes", lista);
        model.addAttribute("nuevasIds", nuevasIds);
        model.addAttribute("esAdmin", Rol.ADMIN.equals(usuario.getRol()));
        model.addAttribute("avisos", avisosService.para(usuario));

        return "mis-solicitudes";
    }

    @PostMapping("/solicitudes/aprobar/{id}")
    public String aprobar(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Solicitud sol = solicitudRepository.findById(id).orElse(null);

        if (sol == null || sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            redirectAttributes.addFlashAttribute("error", "Esa solicitud ya no está disponible.");
            return "redirect:/solicitudes";
        }

        boolean invalida = sol.getCodigo() == null || sol.getCodigo().trim().isEmpty()
                || sol.getNombre() == null || sol.getNombre().trim().isEmpty()
                || sol.getCategoria() == null || sol.getCategoria().trim().isEmpty()
                || sol.getPrecio() == null || sol.getPrecio() <= 0
                || sol.getCantidad() == null || sol.getCantidad() < 0;

        if (invalida) {
            redirectAttributes.addFlashAttribute("error", "Esta solicitud tiene datos inválidos y no se puede aprobar.");
            return "redirect:/solicitudes";
        }

        Producto existente = productoRepository.findByCodigoIgnoreCase(sol.getCodigo().trim());
        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");

        if (sol.getAccion() == AccionSolicitud.CREAR_PRODUCTO) {

            if (existente != null) {
                redirectAttributes.addFlashAttribute("error", "Ya existe un producto con ese código, no se puede aprobar esta solicitud.");
                return "redirect:/solicitudes";
            }

            Producto nuevo = new Producto();
            nuevo.setCodigo(sol.getCodigo().trim());
            nuevo.setNombre(sol.getNombre().trim());
            nuevo.setCategoria(sol.getCategoria().trim());
            nuevo.setPrecio(sol.getPrecio());
            nuevo.setCantidad(sol.getCantidad());
            productoRepository.save(nuevo);

            try {
                categoriaService.aprender(nuevo.getNombre(), nuevo.getCategoria(), nuevo.getId());
            } catch (Exception e) {
                // aprender es un extra: si falla, el producto ya quedó creado
            }

            historialService.registrar(usuarioActual,
                    "APROBÓ SOLICITUD DE " + sol.getUsuarioSolicitante().getUsername() + ": CREÓ PRODUCTO: " + nuevo.getNombre());

        } else {

            Producto producto = sol.getProducto();

            if (producto == null) {
                redirectAttributes.addFlashAttribute("error", "El producto de esta solicitud ya no existe.");
                return "redirect:/solicitudes";
            }

            if (existente != null && !existente.getId().equals(producto.getId())) {
                redirectAttributes.addFlashAttribute("error", "Ya existe otro producto con ese código, no se puede aprobar esta solicitud.");
                return "redirect:/solicitudes";
            }

            producto.setCodigo(sol.getCodigo().trim());
            producto.setNombre(sol.getNombre().trim());
            producto.setCategoria(sol.getCategoria().trim());
            producto.setPrecio(sol.getPrecio());
            producto.setCantidad(sol.getCantidad());
            productoRepository.save(producto);

            historialService.registrar(usuarioActual,
                    "APROBÓ SOLICITUD DE " + sol.getUsuarioSolicitante().getUsername() + ": EDITÓ PRODUCTO: " + producto.getNombre());
        }

        sol.setEstado(EstadoSolicitud.APROBADA);
        sol.setVistaPorSolicitante(false);
        solicitudRepository.save(sol);

        redirectAttributes.addFlashAttribute("mensaje", "Solicitud aprobada correctamente.");
        return "redirect:/solicitudes";
    }

    @PostMapping("/solicitudes/rechazar/{id}")
    public String rechazar(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/";
        }

        Solicitud sol = solicitudRepository.findById(id).orElse(null);

        if (sol == null || sol.getEstado() != EstadoSolicitud.PENDIENTE) {
            redirectAttributes.addFlashAttribute("error", "Esa solicitud ya no está disponible.");
            return "redirect:/solicitudes";
        }

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");

        sol.setEstado(EstadoSolicitud.RECHAZADA);
        sol.setVistaPorSolicitante(false);
        solicitudRepository.save(sol);

        historialService.registrar(usuarioActual,
                "RECHAZÓ SOLICITUD DE " + sol.getUsuarioSolicitante().getUsername() + ": "
                        + (sol.getAccion() == AccionSolicitud.CREAR_PRODUCTO ? "CREAR" : "EDITAR")
                        + " PRODUCTO: " + sol.getNombre());

        redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada.");
        return "redirect:/solicitudes";
    }
}