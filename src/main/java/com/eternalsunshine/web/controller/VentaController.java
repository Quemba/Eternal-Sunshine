package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.UsuarioRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import com.eternalsunshine.web.service.PaginaVista;
import com.eternalsunshine.web.service.VentaConsultaService;
import com.eternalsunshine.web.service.VentaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class VentaController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VentaService ventaService;

    @Autowired
    private VentaConsultaService ventaConsultaService;

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    private LocalDate leerFecha(String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    @GetMapping("/ventas/nueva")
    public String nuevaVenta(Model model, HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("esAdmin", esAdmin(usuario));

        return "venta-nueva";
    }

    @PostMapping("/ventas/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrarVenta(
            @RequestBody List<VentaService.CarritoItem> items,
            HttpSession session) {

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");
        Map<String, Object> respuesta = new HashMap<>();

        try {
            Venta venta = ventaService.registrarVenta(items, usuarioActual);
            respuesta.put("ok", true);
            respuesta.put("id", venta.getId());
            return ResponseEntity.ok(respuesta);

        } catch (IllegalArgumentException e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);

        } catch (Exception e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", "Ocurrió un error al registrar la venta.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @GetMapping("/ventas")
    public String listar(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) Integer vendedor,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String desde,
            @RequestParam(defaultValue = "") String hasta,
            @RequestParam(defaultValue = "1") int pagina,
            Model model,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        boolean admin = esAdmin(usuario);

        Integer vendedorFiltro = admin ? vendedor : usuario.getId();

        PaginaVista<Venta> resultado = ventaConsultaService.buscar(
                q, vendedorFiltro, estado, leerFecha(desde), leerFecha(hasta), pagina);

        model.addAttribute("resultado", resultado);
        model.addAttribute("esAdmin", admin);

        if (admin) {
            model.addAttribute("usuarios", usuarioRepository.findAll(Sort.by("username")));
        }

        model.addAttribute("q", q.trim());
        model.addAttribute("vendedorId", admin ? vendedor : null);
        model.addAttribute("estado", estado.trim());
        model.addAttribute("desde", desde.trim());
        model.addAttribute("hasta", hasta.trim());

        boolean hayFiltros = !q.trim().isEmpty() || (admin && vendedor != null)
                || !estado.trim().isEmpty() || !desde.trim().isEmpty() || !hasta.trim().isEmpty();
        model.addAttribute("hayFiltros", hayFiltros);

        return "ventas";
    }

    @GetMapping("/ventas/{id}")
    public String detalle(
            @PathVariable Integer id,
            @RequestParam(required = false) String ok,
            Model model,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        boolean admin = esAdmin(usuario);

        Venta venta = ventaRepository.findById(id).orElse(null);

        if (venta == null) {
            return "redirect:/ventas";
        }

        if (!admin && (venta.getUsuario() == null || !venta.getUsuario().getId().equals(usuario.getId()))) {
            return "redirect:/ventas";
        }

        model.addAttribute("venta", venta);
        model.addAttribute("recienCreada", ok != null);
        model.addAttribute("esAdmin", admin);

        return "venta-detalle";
    }

    @PostMapping("/ventas/{id}/anular")
    public String anular(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "") String motivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (!esAdmin(usuario)) {
            return "redirect:/ventas";
        }

        try {
            ventaService.anular(id, motivo, usuario);
            redirectAttributes.addFlashAttribute("mensaje",
                    "Venta anulada. Las unidades volvieron al inventario y la venta ya no cuenta en los totales.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/ventas/" + id;
    }
}