package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.*;
import com.eternalsunshine.web.repository.DetalleConteoRepository;
import com.eternalsunshine.web.repository.DetalleVentaRepository;
import com.eternalsunshine.web.repository.EntradaMercanciaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.SolicitudRepository;
import com.eternalsunshine.web.service.CategoriaService;
import com.eternalsunshine.web.service.HistorialService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProductoFormController {

    private static final Logger log = LoggerFactory.getLogger(ProductoFormController.class);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private SolicitudRepository solicitudRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private EntradaMercanciaRepository entradaRepository;

    @Autowired
    private DetalleConteoRepository detalleConteoRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private CategoriaService categoriaService;

    private boolean esAdmin(HttpSession session) {
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    private boolean esInvalido(Producto producto) {
        return producto.getCodigo() == null || producto.getCodigo().trim().isEmpty() || producto.getCodigo().length() > 50
                || producto.getNombre() == null || producto.getNombre().trim().isEmpty() || producto.getNombre().length() > 150
                || producto.getCategoria() == null || producto.getCategoria().trim().isEmpty() || producto.getCategoria().length() > 100
                || producto.getPrecio() == null || producto.getPrecio() <= 0
                || producto.getCantidad() == null || producto.getCantidad() < 0;
    }

    private void prepararFormulario(Model model) {
        model.addAttribute("categorias", categoriaService.opciones());
    }

    @GetMapping("/productos/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("producto", new Producto());
        prepararFormulario(model);
        return "producto-form";
    }

    @GetMapping("/productos/sugerir-categoria")
    @ResponseBody
    public Map<String, Object> sugerirCategoria(@RequestParam(defaultValue = "") String nombre) {

        Map<String, Object> respuesta = new HashMap<>();
        String limpio = nombre.trim();

        respuesta.put("categoria", null);
        respuesta.put("motivo", null);

        if (limpio.length() < 3 || limpio.length() > 150) {
            return respuesta;
        }

        try {
            CategoriaService.Sugerencia sugerencia = categoriaService.sugerir(limpio);

            if (sugerencia != null) {
                respuesta.put("categoria", sugerencia.getCategoria());
                respuesta.put("motivo", sugerencia.getMotivo());
            }

        } catch (Exception e) {
            log.error("No se pudo calcular la categoría recomendada para «{}»: {}", limpio, e.toString(), e);
        }

        return respuesta;
    }

    @GetMapping("/productos/categoria-existente")
    @ResponseBody
    public Map<String, Object> categoriaExistente(@RequestParam(defaultValue = "") String texto) {

        Map<String, Object> respuesta = new HashMap<>();
        String limpio = texto.trim();

        String existente = null;

        if (limpio.length() >= 2 && limpio.length() <= 100) {
            try {
                existente = categoriaService.existente(limpio);
            } catch (Exception e) {
                log.error("No se pudo comprobar si la categoría «{}» existe: {}", limpio, e.toString(), e);
            }
        }

        respuesta.put("existente", existente);

        return respuesta;
    }

    @PostMapping("/productos/eliminar/{id}")
    public String eliminarProducto(
            @PathVariable Integer id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!esAdmin(session)) {
            return "redirect:/productos";
        }

        Producto producto = productoRepository.findById(id).orElse(null);

        if (producto == null) {
            return "redirect:/productos";
        }

        if (solicitudRepository.existsByProducto_IdAndEstado(id, EstadoSolicitud.PENDIENTE)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar este producto porque tiene una solicitud pendiente de aprobación.");
            return "redirect:/productos";
        }

        if (detalleVentaRepository.existsByProducto_Id(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar este producto porque tiene ventas registradas. Si ya no se vende, déjalo con cantidad 0.");
            return "redirect:/productos";
        }

        if (entradaRepository.existsByProducto_Id(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar este producto porque tiene entradas de mercancía registradas. Si ya no se vende, déjalo con cantidad 0.");
            return "redirect:/productos";
        }

        if (detalleConteoRepository.existsByProducto_Id(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar este producto porque está incluido en conteos de inventario. Si ya no se vende, déjalo con cantidad 0.");
            return "redirect:/productos";
        }

        List<Solicitud> solicitudesAntiguas = solicitudRepository.findByProducto_Id(id);
        for (Solicitud s : solicitudesAntiguas) {
            s.setProducto(null);
        }
        solicitudRepository.saveAll(solicitudesAntiguas);

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");
        historialService.registrar(usuarioActual, "ELIMINÓ PRODUCTO: " + producto.getNombre());

        productoRepository.deleteById(id);
        return "redirect:/productos";
    }

    @GetMapping("/productos/editar/{id}")
    public String editarProducto(
            @PathVariable Integer id,
            Model model) {

        Producto producto = productoRepository.findById(id).orElse(null);

        if (producto == null) {
            return "redirect:/productos";
        }

        model.addAttribute("producto", producto);
        prepararFormulario(model);
        return "producto-form";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(
            @ModelAttribute Producto producto,
            @RequestParam(required = false) String categoriaOtra,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        boolean esOtra = CategoriaService.VALOR_OTRA.equals(producto.getCategoria());

        if (esOtra) {

            String yaExiste = categoriaService.existente(categoriaOtra);

            if (yaExiste != null) {
                producto.setCategoria(yaExiste);
                model.addAttribute("producto", producto);
                prepararFormulario(model);
                model.addAttribute("error",
                        "La categoría «" + yaExiste + "» ya existe. La dejé seleccionada: revisa el formulario y guarda de nuevo.");
                return "producto-form";
            }
        }

        String categoriaElegida = esOtra ? categoriaOtra : producto.getCategoria();

        producto.setCategoria(categoriaService.canonica(categoriaElegida));

        if (esInvalido(producto)) {
            model.addAttribute("producto", producto);
            prepararFormulario(model);
            model.addAttribute("error",
                    "Revisa los datos: todos los campos son obligatorios, el código no puede superar 50 caracteres, el nombre 150, la categoría 100, el precio debe ser mayor a 0, y la cantidad no puede ser negativa.");
            return "producto-form";
        }

        producto.setCodigo(producto.getCodigo().trim());
        producto.setNombre(producto.getNombre().trim());
        producto.setCategoria(producto.getCategoria().trim());

        boolean nuevo = (producto.getId() == null);

        Producto existente = productoRepository.findByCodigoIgnoreCase(producto.getCodigo());
        if (existente != null && (nuevo || !existente.getId().equals(producto.getId()))) {
            model.addAttribute("producto", producto);
            prepararFormulario(model);
            model.addAttribute("error", "Ya existe un producto con ese código.");
            return "producto-form";
        }

        Usuario usuarioActual = (Usuario) session.getAttribute("usuarioLogueado");

        if (!Rol.ADMIN.equals(usuarioActual.getRol())) {

            boolean yaTieneSolicitudPendiente = nuevo
                    ? solicitudRepository.existsByCodigoIgnoreCaseAndEstado(producto.getCodigo(), EstadoSolicitud.PENDIENTE)
                    : solicitudRepository.existsByProducto_IdAndEstado(producto.getId(), EstadoSolicitud.PENDIENTE);

            if (yaTieneSolicitudPendiente) {
                model.addAttribute("producto", producto);
                prepararFormulario(model);
                model.addAttribute("error", "Ya tienes una solicitud pendiente sobre este producto. Espera a que el administrador la revise antes de enviar otra.");
                return "producto-form";
            }

            Solicitud sol = new Solicitud();
            sol.setAccion(nuevo ? AccionSolicitud.CREAR_PRODUCTO : AccionSolicitud.EDITAR_PRODUCTO);
            sol.setProducto(nuevo ? null : productoRepository.findById(producto.getId()).orElse(null));
            sol.setCodigo(producto.getCodigo());
            sol.setNombre(producto.getNombre());
            sol.setCategoria(producto.getCategoria());
            sol.setPrecio(producto.getPrecio());
            sol.setCantidad(producto.getCantidad());
            sol.setEstado(EstadoSolicitud.PENDIENTE);
            sol.setUsuarioSolicitante(usuarioActual);
            sol.setFechaSolicitud(LocalDateTime.now());
            solicitudRepository.save(sol);

            historialService.registrar(usuarioActual,
                    (nuevo ? "SOLICITÓ CREAR PRODUCTO: " : "SOLICITÓ EDITAR PRODUCTO: ") + producto.getNombre());

            redirectAttributes.addFlashAttribute("mensaje", "Tu solicitud fue enviada para aprobación del administrador.");
            return "redirect:/productos";
        }

        if (!nuevo && solicitudRepository.existsByProducto_IdAndEstado(producto.getId(), EstadoSolicitud.PENDIENTE)) {
            model.addAttribute("producto", producto);
            prepararFormulario(model);
            model.addAttribute("error", "Este producto tiene una solicitud pendiente de aprobación. Revísala primero en Solicitudes.");
            return "producto-form";
        }

        Integer cantidadAnterior = null;

        if (!nuevo) {
            Producto anterior = productoRepository.findById(producto.getId()).orElse(null);
            if (anterior != null) {
                cantidadAnterior = anterior.getCantidad();
            }
        }

        productoRepository.save(producto);

        if (nuevo) {
            historialService.registrar(usuarioActual, "CREÓ PRODUCTO: " + producto.getNombre());

            try {
                categoriaService.aprender(producto.getNombre(), producto.getCategoria(), producto.getId());
            } catch (Exception e) {
                log.warn("No se pudo registrar el aprendizaje de categoría de «{}»: {}",
                        producto.getNombre(), e.toString());
            }

        } else {
            historialService.registrar(usuarioActual, "EDITÓ PRODUCTO: " + producto.getNombre());

            if (cantidadAnterior != null && !cantidadAnterior.equals(producto.getCantidad())) {
                historialService.registrar(usuarioActual,
                        "CORRIGIÓ STOCK de " + producto.getNombre() + ": " + cantidadAnterior + " → " + producto.getCantidad());
            }
        }

        return "redirect:/productos";
    }
}