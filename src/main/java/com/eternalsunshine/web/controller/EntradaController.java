package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.EntradaMercancia;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.service.EntradaService;
import com.eternalsunshine.web.service.PaginaVista;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class EntradaController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EntradaService entradaService;

    public static class SolicitudEntrada {
        private String nota;
        private String proveedor;
        private String numeroFactura;
        private String fechaFactura;
        private Boolean confirmarDuplicada;
        private List<EntradaService.LineaEntrada> lineas;

        public String getNota() { return nota; }
        public void setNota(String nota) { this.nota = nota; }
        public String getProveedor() { return proveedor; }
        public void setProveedor(String proveedor) { this.proveedor = proveedor; }
        public String getNumeroFactura() { return numeroFactura; }
        public void setNumeroFactura(String numeroFactura) { this.numeroFactura = numeroFactura; }
        public String getFechaFactura() { return fechaFactura; }
        public void setFechaFactura(String fechaFactura) { this.fechaFactura = fechaFactura; }
        public Boolean getConfirmarDuplicada() { return confirmarDuplicada; }
        public void setConfirmarDuplicada(Boolean confirmarDuplicada) { this.confirmarDuplicada = confirmarDuplicada; }
        public List<EntradaService.LineaEntrada> getLineas() { return lineas; }
        public void setLineas(List<EntradaService.LineaEntrada> lineas) { this.lineas = lineas; }
    }

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

    @GetMapping("/entradas")
    public String listar(
            @RequestParam(required = false) Integer registradas,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String proveedor,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "") String desde,
            @RequestParam(defaultValue = "") String hasta,
            @RequestParam(defaultValue = "1") int pagina,
            Model model,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        PaginaVista<EntradaMercancia> resultado = entradaService.buscar(
                q, proveedor, estado, leerFecha(desde), leerFecha(hasta), pagina);

        model.addAttribute("resultado", resultado);
        model.addAttribute("proveedores", entradaService.proveedores());

        model.addAttribute("q", q.trim());
        model.addAttribute("proveedorSel", proveedor.trim());
        model.addAttribute("estado", estado.trim());
        model.addAttribute("desde", desde.trim());
        model.addAttribute("hasta", hasta.trim());

        boolean hayFiltros = !q.trim().isEmpty() || !proveedor.trim().isEmpty()
                || !estado.trim().isEmpty() || !desde.trim().isEmpty() || !hasta.trim().isEmpty();
        model.addAttribute("hayFiltros", hayFiltros);

        model.addAttribute("esAdmin", esAdmin(usuario));

        if (registradas != null && registradas > 0) {

            String numero = usuario == null ? null : entradaService.numeroDeLaUltimaEntrada(usuario.getId());

            model.addAttribute("mensaje",
                    (numero != null ? "Entrada " + numero + " registrada: " : "Entrada registrada: ")
                            + (registradas == 1 ? "1 producto." : registradas + " productos."));
        }

        return "entradas";
    }

    @GetMapping("/entradas/nueva")
    public String nueva(
            @RequestParam(required = false) Integer producto,
            @RequestParam(required = false) Integer cantidad,
            Model model,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        model.addAttribute("productos", productoRepository.findAll());
        model.addAttribute("proveedores", entradaService.proveedores());
        model.addAttribute("preProducto", producto);
        model.addAttribute("preCantidad", cantidad);
        model.addAttribute("esAdmin", esAdmin(usuario));

        return "entrada-nueva";
    }

    @PostMapping("/entradas/registrar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> registrar(
            @RequestBody SolicitudEntrada solicitud,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        Map<String, Object> respuesta = new HashMap<>();

        try {
            LocalDate fechaFactura = null;
            String textoFecha = solicitud.getFechaFactura();

            if (textoFecha != null && !textoFecha.trim().isEmpty()) {
                try {
                    fechaFactura = LocalDate.parse(textoFecha.trim());
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("La fecha de la factura no es válida.");
                }
            }

            List<EntradaMercancia> creadas = entradaService.registrar(
                    solicitud.getLineas(),
                    solicitud.getNota(),
                    solicitud.getProveedor(),
                    solicitud.getNumeroFactura(),
                    fechaFactura,
                    Boolean.TRUE.equals(solicitud.getConfirmarDuplicada()),
                    usuario);

            respuesta.put("ok", true);
            respuesta.put("cantidad", creadas.size());
            respuesta.put("numero", creadas.get(0).getNumero());
            return ResponseEntity.ok(respuesta);

        } catch (EntradaService.FacturaDuplicadaException e) {
            respuesta.put("ok", false);
            respuesta.put("duplicada", true);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);

        } catch (IllegalArgumentException e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", e.getMessage());
            return ResponseEntity.badRequest().body(respuesta);

        } catch (Exception e) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", "Ocurrió un error al registrar la entrada.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
        }
    }

    @PostMapping("/entradas/anular/{id}")
    public String anular(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");

        if (!esAdmin(usuario)) {
            return "redirect:/entradas";
        }

        try {
            entradaService.anular(id, usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Entrada anulada. El stock fue revertido.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/entradas";
    }
}