package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.ConteoInventario;
import com.eternalsunshine.web.entity.EstadoConteo;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.ConteoRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.service.ConteoService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Controller
public class ConteoController {

    public static class OperacionConteo {

        private Integer productoId;
        private Integer cantidad;

        public Integer getProductoId() { return productoId; }
        public void setProductoId(Integer productoId) { this.productoId = productoId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }

    @Autowired
    private ConteoService conteoService;

    @Autowired
    private ConteoRepository conteoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    private Usuario usuario(HttpSession session) {
        return (Usuario) session.getAttribute("usuarioLogueado");
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    private ResponseEntity<Map<String, Object>> ejecutar(Supplier<ConteoService.ResultadoConteo> accion) {

        Map<String, Object> respuesta = new HashMap<>();

        try {
            ConteoService.ResultadoConteo resultado = accion.get();

            respuesta.put("ok", true);
            respuesta.put("lineas", resultado.getLineas());
            respuesta.put("totalProductos", resultado.getTotalProductos());
            respuesta.put("totalUnidades", resultado.getTotalUnidades());

            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);

        } catch (Exception e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", "Ocurrió un error al guardar el conteo.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @GetMapping("/conteos")
    public String listar(
            @RequestParam(defaultValue = "") String estado,
            Model model,
            HttpSession session) {

        Usuario usuario = usuario(session);

        String filtro = List.of("en_curso", "por_revisar", "revisados", "cancelados").contains(estado) ? estado : "";

        model.addAttribute("conteos", conteoService.listar(usuario, filtro));
        model.addAttribute("filtro", filtro);
        model.addAttribute("esAdmin", esAdmin(usuario));

        return "conteos";
    }

    @PostMapping("/conteos/nuevo")
    public String nuevo(
            @RequestParam(defaultValue = "") String nombre,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            ConteoInventario conteo = conteoService.iniciar(nombre, usuario(session));
            return "redirect:/conteos/" + conteo.getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/conteos";
        }
    }

    @GetMapping("/conteos/{id}")
    public String ver(
            @PathVariable Integer id,
            Model model,
            HttpSession session) {

        Usuario usuario = usuario(session);
        boolean admin = esAdmin(usuario);

        ConteoInventario conteo = conteoRepository.findById(id).orElse(null);

        if (conteo == null) {
            return "redirect:/conteos";
        }

        boolean propio = conteo.getUsuario() != null && usuario != null
                && conteo.getUsuario().getId().equals(usuario.getId());

        if (!admin && !propio) {
            return "redirect:/conteos";
        }

        model.addAttribute("conteo", conteo);
        model.addAttribute("esAdmin", admin);
        model.addAttribute("propio", propio);

        if (conteo.getEstado() == EstadoConteo.EN_CURSO && propio) {

            List<Map<String, Object>> productos = new ArrayList<>();

            for (Producto p : productoRepository.findAll()) {
                Map<String, Object> dato = new HashMap<>();
                dato.put("id", p.getId());
                dato.put("codigo", p.getCodigo() == null ? "" : p.getCodigo());
                dato.put("nombre", p.getNombre() == null ? "" : p.getNombre());
                productos.add(dato);
            }

            model.addAttribute("productos", productos);
            model.addAttribute("lineas", conteoService.resultado(conteo).getLineas());

            return "conteo-contar";
        }

        model.addAttribute("detalle", conteoService.detalle(conteo));

        return "conteo-detalle";
    }

    @PostMapping("/conteos/{id}/contar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> contar(
            @PathVariable Integer id,
            @RequestBody OperacionConteo operacion,
            HttpSession session) {

        Usuario usuario = usuario(session);

        return ejecutar(() -> conteoService.contar(id, operacion.getProductoId(), operacion.getCantidad(), usuario));
    }

    @PostMapping("/conteos/{id}/fijar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> fijar(
            @PathVariable Integer id,
            @RequestBody OperacionConteo operacion,
            HttpSession session) {

        Usuario usuario = usuario(session);

        return ejecutar(() -> conteoService.fijar(id, operacion.getProductoId(), operacion.getCantidad(), usuario));
    }

    @PostMapping("/conteos/{id}/quitar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quitar(
            @PathVariable Integer id,
            @RequestBody OperacionConteo operacion,
            HttpSession session) {

        Usuario usuario = usuario(session);

        return ejecutar(() -> conteoService.quitar(id, operacion.getProductoId(), usuario));
    }

    @PostMapping("/conteos/{id}/enviar")
    public String enviar(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            conteoService.enviar(id, usuario(session));

            ConteoInventario conteo = conteoRepository.findById(id).orElse(null);
            String numero = conteo == null ? null : conteo.getNumero();

            redirectAttributes.addFlashAttribute("mensaje",
                    "Conteo " + (numero != null ? numero + " " : "")
                            + "enviado a revisión. El administrador revisará las diferencias.");
            return "redirect:/conteos";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/conteos/" + id;
        }
    }

    @PostMapping("/conteos/{id}/cancelar")
    public String cancelar(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        try {
            conteoService.cancelar(id, usuario(session));
            redirectAttributes.addFlashAttribute("mensaje",
                    "Conteo cancelado. Quedó registrado en la lista, sin número, y el inventario no se modificó.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/conteos";
    }

    @PostMapping("/conteos/{id}/aplicar")
    public String aplicar(
            @PathVariable Integer id,
            @RequestParam(required = false) List<Integer> lineas,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuario(session);

        if (!esAdmin(usuario)) {
            return "redirect:/conteos";
        }

        try {
            ConteoService.ResultadoRevision resultado = conteoService.aplicarRevision(id, lineas, usuario);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Conteo revisado: " + resultado.getAplicadas()
                            + (resultado.getAplicadas() == 1 ? " ajuste aplicado" : " ajustes aplicados")
                            + ", " + resultado.getSinAjustar() + " sin ajustar.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/conteos/" + id;
    }

    @PostMapping("/conteos/{id}/descartar")
    public String descartar(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = usuario(session);

        if (!esAdmin(usuario)) {
            return "redirect:/conteos";
        }

        try {
            ConteoService.ResultadoRevision resultado = conteoService.aplicarRevision(id, null, usuario);

            redirectAttributes.addFlashAttribute("mensaje",
                    "Conteo cerrado sin ajustar el inventario (" + resultado.getSinAjustar()
                            + (resultado.getSinAjustar() == 1 ? " diferencia" : " diferencias") + " sin corregir).");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/conteos/" + id;
    }
}