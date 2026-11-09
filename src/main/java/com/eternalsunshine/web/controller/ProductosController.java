package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.service.PaginaVista;
import com.eternalsunshine.web.service.ProductoConsultaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProductosController {

    @Autowired
    private ProductoConsultaService productoConsultaService;

    @Autowired
    private ProductoRepository productoRepository;

    @GetMapping("/productos")
    public String mostrarProductos(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "") String categoria,
            @RequestParam(defaultValue = "") String estado,
            @RequestParam(defaultValue = "1") int pagina,
            Model model,
            HttpSession session) {

        PaginaVista<Producto> resultado =
                productoConsultaService.buscar(buscar, categoria, estado, pagina);

        model.addAttribute("resultado", resultado);
        model.addAttribute("categorias", productoConsultaService.categorias());

        model.addAttribute("buscar", buscar.trim());
        model.addAttribute("categoriaSel", categoria.trim());
        model.addAttribute("estado", estado.trim());

        boolean hayFiltros = !buscar.trim().isEmpty()
                || !categoria.trim().isEmpty()
                || !estado.trim().isEmpty();
        model.addAttribute("hayFiltros", hayFiltros);

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("esAdmin", usuario != null && Rol.ADMIN.equals(usuario.getRol()));

        return "productos";
    }

    @GetMapping("/productos/por-agotarse")
    public String porAgotarse(Model model, HttpSession session) {

        List<Producto> lista = productoRepository.findByCantidadLessThanEqualOrderByCantidadAscNombreAsc(5);

        long sinStock = lista.stream()
                .filter(p -> p.getCantidad() != null && p.getCantidad() == 0)
                .count();

        model.addAttribute("productos", lista);
        model.addAttribute("sinStock", sinStock);
        model.addAttribute("stockBajoLista", lista.size() - sinStock);

        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        model.addAttribute("esAdmin", usuario != null && Rol.ADMIN.equals(usuario.getRol()));

        return "productos-por-agotarse";
    }
}