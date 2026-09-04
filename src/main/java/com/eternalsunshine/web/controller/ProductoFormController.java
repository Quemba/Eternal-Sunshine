package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.service.HistorialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProductoFormController {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private HistorialService historialService;

    @GetMapping("/productos/nuevo")
    public String mostrarFormulario(Model model) {

        model.addAttribute(
                "producto",
                new Producto());

        return "producto-form";
    }

    @GetMapping("/productos/eliminar/{id}")
    public String eliminarProducto(
            @PathVariable Integer id) {

        Producto producto =
                productoRepository
                        .findById(id)
                        .orElse(null);

        if (producto != null) {

            historialService.registrar(
                    "admin",
                    "ELIMINÓ PRODUCTO: "
                            + producto.getNombre());
        }

        productoRepository.deleteById(id);

        return "redirect:/productos";
    }

    @GetMapping("/productos/editar/{id}")
    public String editarProducto(
            @PathVariable Integer id,
            Model model) {

        Producto producto =
                productoRepository
                        .findById(id)
                        .orElse(null);

        model.addAttribute(
                "producto",
                producto);

        return "producto-form";
    }

    @PostMapping("/productos/guardar")
    public String guardarProducto(
            @ModelAttribute Producto producto) {

        boolean nuevo =
                (producto.getId() == null);

        productoRepository.save(producto);

        if (nuevo) {

            historialService.registrar(
                    "admin",
                    "CREÓ PRODUCTO: "
                            + producto.getNombre());

        } else {

            historialService.registrar(
                    "admin",
                    "EDITÓ PRODUCTO: "
                            + producto.getNombre());
        }

        return "redirect:/productos";
    }
}
