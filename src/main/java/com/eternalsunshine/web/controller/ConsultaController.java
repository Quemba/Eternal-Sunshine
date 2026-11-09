package com.eternalsunshine.web.controller;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.ProductoBusquedaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.service.ProductoConsultaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ConsultaController {

    public static class ProductoVista {

        private final Integer id;
        private final String codigo;
        private final String nombre;
        private final String categoria;
        private final int precio;
        private final int cantidad;
        private final String estado;

        public ProductoVista(Integer id, String codigo, String nombre, String categoria,
                             int precio, int cantidad, String estado) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.cantidad = cantidad;
            this.estado = estado;
        }

        public Integer getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getCategoria() { return categoria; }
        public int getPrecio() { return precio; }
        public int getCantidad() { return cantidad; }
        public String getEstado() { return estado; }
    }

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoBusquedaRepository busquedaRepository;

    @Autowired
    private ProductoConsultaService productoConsultaService;

    private ProductoVista vista(Producto producto) {

        int cantidad = producto.getCantidad() == null ? 0 : producto.getCantidad();
        int precio = producto.getPrecio() == null ? 0 : producto.getPrecio();

        String estado = cantidad == 0 ? "agotado" : (cantidad <= 5 ? "bajo" : "disponible");

        return new ProductoVista(
                producto.getId(),
                producto.getCodigo() == null ? "" : producto.getCodigo(),
                producto.getNombre() == null ? "" : producto.getNombre(),
                producto.getCategoria() == null ? "" : producto.getCategoria(),
                precio,
                cantidad,
                estado);
    }

    @GetMapping("/consulta/codigo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> porCodigo(
            @RequestParam(defaultValue = "") String codigo) {

        Map<String, Object> respuesta = new HashMap<>();
        String limpio = codigo.trim();

        if (limpio.isEmpty() || limpio.length() > 50) {
            respuesta.put("ok", false);
            respuesta.put("mensaje", "Código no válido.");
            return ResponseEntity.badRequest().body(respuesta);
        }

        Producto exacto = productoRepository.findByCodigoIgnoreCase(limpio);

        if (exacto != null) {
            respuesta.put("ok", true);
            respuesta.put("producto", vista(exacto));
            return ResponseEntity.ok(respuesta);
        }

        respuesta.put("ok", false);
        respuesta.put("mensaje", "No hay un producto registrado con el código " + limpio + ".");

        if (limpio.matches("\\d{13}")) {

            List<Producto> parecidos = new ArrayList<>();

            for (Producto p : busquedaRepository.findByCodigoEndingWith(limpio.substring(7))) {
                if (p.getCodigo() != null && p.getCodigo().matches("\\d{13}")) {
                    parecidos.add(p);
                }
            }

            if (parecidos.size() == 1) {
                respuesta.put("sugerencia", vista(parecidos.get(0)));
            }
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @GetMapping("/consulta/buscar")
    @ResponseBody
    public Map<String, Object> buscar(@RequestParam(defaultValue = "") String q) {

        String limpio = q.trim();
        List<ProductoVista> lista = new ArrayList<>();

        if (!limpio.isEmpty() && limpio.length() <= 100) {

            for (Producto p : productoConsultaService.buscar(limpio, "", "", 1).getFilas()) {
                lista.add(vista(p));

                if (lista.size() == 8) {
                    break;
                }
            }
        }

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("productos", lista);

        return respuesta;
    }
}