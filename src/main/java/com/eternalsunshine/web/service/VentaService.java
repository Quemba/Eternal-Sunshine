package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.DetalleVenta;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
public class VentaService {

    private static final long STOCK_MAXIMO = 1000000L;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private NumeracionService numeracionService;

    public static class CarritoItem {
        private Integer productoId;
        private Integer cantidad;

        public Integer getProductoId() { return productoId; }
        public void setProductoId(Integer productoId) { this.productoId = productoId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }

    private String cop(int valor) {
        return "$" + String.format(Locale.US, "%,d", valor).replace(',', '.');
    }

    private String recortar(String texto) {
        return texto.length() > 250 ? texto.substring(0, 247) + "..." : texto;
    }

    private int nz(Integer valor) {
        return valor == null ? 0 : valor;
    }

    @Transactional
    public Venta registrarVenta(List<CarritoItem> items, Usuario usuario) {

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        Map<Integer, Integer> cantidades = new LinkedHashMap<>();
        for (CarritoItem item : items) {
            if (item.getProductoId() == null || item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new IllegalArgumentException("El carrito tiene datos inválidos.");
            }
            cantidades.merge(item.getProductoId(), item.getCantidad(), Integer::sum);
        }

        Map<Integer, Producto> bloqueados = new HashMap<>();
        for (Integer productoId : new TreeSet<>(cantidades.keySet())) {
            Producto bloqueado = productoRepository.buscarParaActualizar(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos del carrito ya no existe."));
            bloqueados.put(productoId, bloqueado);
        }

        LocalDateTime ahora = LocalDateTime.now();

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFecha(ahora);
        venta.setAnulada(false);

        int total = 0;
        List<DetalleVenta> detalles = new ArrayList<>();
        List<Producto> productosActualizados = new ArrayList<>();

        for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {

            Producto producto = bloqueados.get(entrada.getKey());
            int cantidad = entrada.getValue();

            if (cantidad > producto.getCantidad()) {
                throw new IllegalArgumentException("No hay suficiente stock de " + producto.getNombre()
                        + " (disponible: " + producto.getCantidad() + ").");
            }

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(producto.getPrecio());

            int subtotal = producto.getPrecio() * cantidad;
            detalle.setSubtotal(subtotal);
            total += subtotal;

            detalles.add(detalle);

            producto.setCantidad(producto.getCantidad() - cantidad);
            productosActualizados.add(producto);
        }

        venta.setTotal(total);
        venta.setDetalles(detalles);

        venta.setPeriodo(NumeracionService.periodo(ahora));
        venta.setConsecutivo(numeracionService.siguiente(TipoDocumento.VENTA, ahora));

        Venta guardada = ventaRepository.save(venta);
        productoRepository.saveAll(productosActualizados);

        StringBuilder resumen = new StringBuilder("VENTA " + guardada.getNumero() + ": ");
        for (int i = 0; i < detalles.size(); i++) {
            DetalleVenta d = detalles.get(i);
            resumen.append(d.getCantidad()).append("x ").append(d.getProducto().getNombre());
            if (i < detalles.size() - 1) resumen.append(", ");
        }
        resumen.append(" - Total: ").append(cop(total));

        historialService.registrar(usuario, recortar(resumen.toString()));

        return guardada;
    }

    @Transactional
    public Venta anular(Integer ventaId, String motivo, Usuario admin) {

        if (admin == null || !Rol.ADMIN.equals(admin.getRol())) {
            throw new IllegalArgumentException("Solo el administrador puede anular una venta.");
        }

        String motivoLimpio = motivo == null ? "" : motivo.trim().replaceAll("\\s+", " ");

        if (motivoLimpio.length() < 3) {
            throw new IllegalArgumentException("Escribe el motivo de la anulación (mínimo 3 caracteres).");
        }

        if (motivoLimpio.length() > 150) {
            throw new IllegalArgumentException("El motivo no puede superar 150 caracteres.");
        }

        Venta venta = ventaRepository.buscarParaActualizar(ventaId)
                .orElseThrow(() -> new IllegalArgumentException("Esa venta ya no existe."));

        if (Boolean.TRUE.equals(venta.getAnulada())) {
            throw new IllegalArgumentException("Esa venta ya fue anulada.");
        }

        Map<Integer, Integer> devolver = new TreeMap<>();

        for (DetalleVenta detalle : venta.getDetalles()) {

            if (detalle.getProducto() == null) {
                throw new IllegalArgumentException("No se puede anular: uno de los productos de esa venta ya no existe.");
            }

            devolver.merge(detalle.getProducto().getId(), nz(detalle.getCantidad()), Integer::sum);
        }

        Map<Integer, Producto> productos = new HashMap<>();

        for (Map.Entry<Integer, Integer> entrada : devolver.entrySet()) {

            Producto producto = productoRepository.buscarParaActualizar(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos de esa venta ya no existe."));

            long nuevo = (long) nz(producto.getCantidad()) + entrada.getValue();

            if (nuevo > STOCK_MAXIMO) {
                throw new IllegalArgumentException("No se puede anular: el stock de " + producto.getNombre()
                        + " superaría el máximo permitido (1.000.000).");
            }

            productos.put(entrada.getKey(), producto);
        }

        for (Map.Entry<Integer, Integer> entrada : devolver.entrySet()) {

            Producto producto = productos.get(entrada.getKey());
            producto.setCantidad(nz(producto.getCantidad()) + entrada.getValue());
            productoRepository.save(producto);
        }

        venta.setAnulada(true);
        venta.setUsuarioAnula(admin);
        venta.setFechaAnulacion(LocalDateTime.now());
        venta.setMotivoAnulacion(motivoLimpio);
        ventaRepository.save(venta);

        StringBuilder resumen = new StringBuilder("ANULÓ VENTA " + venta.getNumero() + ": ");

        List<DetalleVenta> detalles = venta.getDetalles();

        for (int i = 0; i < detalles.size(); i++) {
            DetalleVenta d = detalles.get(i);
            resumen.append(d.getCantidad()).append("x ").append(d.getProducto().getNombre());
            if (i < detalles.size() - 1) resumen.append(", ");
        }

        resumen.append(" - Total: ").append(cop(nz(venta.getTotal())))
                .append(" · Motivo: ").append(motivoLimpio);

        historialService.registrar(admin, recortar(resumen.toString()));

        return venta;
    }
}