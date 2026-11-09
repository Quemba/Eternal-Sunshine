package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.DetalleVentaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import com.eternalsunshine.web.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReabastecimientoService {

    public static final int VENTANA_DIAS = 30;
    public static final int COBERTURA_DIAS = 14;
    public static final int ALERTA_DIAS = 7;
    public static final int URGENTE_DIAS = 3;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    public static class Sugerencia {

        private final Producto producto;
        private final int vendidas;
        private final double promedioDiario;
        private final double diasRestantes;
        private final int sugerido;
        private final boolean urgente;

        public Sugerencia(Producto producto, int vendidas, double promedioDiario,
                          double diasRestantes, int sugerido, boolean urgente) {
            this.producto = producto;
            this.vendidas = vendidas;
            this.promedioDiario = promedioDiario;
            this.diasRestantes = diasRestantes;
            this.sugerido = sugerido;
            this.urgente = urgente;
        }

        public Producto getProducto() { return producto; }
        public int getVendidas() { return vendidas; }
        public double getPromedioDiario() { return promedioDiario; }
        public double getDiasRestantes() { return diasRestantes; }
        public int getSugerido() { return sugerido; }
        public boolean isUrgente() { return urgente; }
    }

    public List<Sugerencia> calcular() {

        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(VENTANA_DIAS).atStartOfDay();

        LocalDateTime primeraVenta = ventaRepository.primeraVentaDesde(desde);

        if (primeraVenta == null) {
            return new ArrayList<>();
        }

        long diasConDatos = ChronoUnit.DAYS.between(primeraVenta.toLocalDate(), hoy) + 1;
        diasConDatos = Math.max(1, Math.min(diasConDatos, VENTANA_DIAS));

        Map<Integer, Long> vendidasPorProducto = new HashMap<>();
        for (Object[] fila : detalleVentaRepository.unidadesPorProductoDesde(desde)) {
            vendidasPorProducto.put(((Number) fila[0]).intValue(), ((Number) fila[1]).longValue());
        }

        List<Sugerencia> resultado = new ArrayList<>();

        for (Producto p : productoRepository.findAll()) {

            Long vendidas = vendidasPorProducto.get(p.getId());

            if (vendidas == null || vendidas <= 0) {
                continue;
            }

            double promedio = (double) vendidas / diasConDatos;
            int stock = p.getCantidad() == null ? 0 : p.getCantidad();
            double diasRestantes = stock / promedio;

            if (diasRestantes > ALERTA_DIAS) {
                continue;
            }

            int objetivo = (int) Math.ceil(promedio * COBERTURA_DIAS);
            int sugerido = Math.max(0, objetivo - stock);

            if (sugerido == 0) {
                continue;
            }

            boolean urgente = stock == 0 || diasRestantes <= URGENTE_DIAS;

            resultado.add(new Sugerencia(p, vendidas.intValue(), promedio, diasRestantes, sugerido, urgente));
        }

        resultado.sort(Comparator.comparingDouble(Sugerencia::getDiasRestantes));

        return resultado;
    }
}