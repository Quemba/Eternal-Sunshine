package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class ProductosController {

    @Autowired
    private ProductoService productoService;

    @GetMapping("/productos")
    public String mostrarProductos(
            @RequestParam(required = false)
            String buscar,
            Model model) {

        List<Producto> productos;

        if (buscar != null &&
                !buscar.trim().isEmpty()) {

            productos =
                    productoService.buscar(
                            buscar);

        } else {

            productos =
                    productoService.obtenerTodos();
        }

        model.addAttribute(
                "productos",
                productos);

        model.addAttribute(
                "buscar",
                buscar);

        return "productos";
    }
}