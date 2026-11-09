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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventarioReporteService {

    public static final int VENTANA_DIAS = 30;

    private static final double LIMITE_A = 0.80;
    private static final double LIMITE_B = 0.95;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoConsultaService productoConsultaService;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private VentaRepository ventaRepository;

    public static class FilaInventario {

        private final String codigo;
        private final String nombre;
        private final String categoria;
        private final int precio;
        private final int cantidad;
        private final long valor;
        private final String estado;
        private final int vendidas;
        private final Double diasStock;
        private final double participacion;
        private final String claseAbc;

        public FilaInventario(String codigo, String nombre, String categoria, int precio, int cantidad,
                              long valor, String estado, int vendidas, Double diasStock,
                              double participacion, String claseAbc) {
            this.codigo = codigo;
            this.nombre = nombre;
            this.categoria = categoria;
            this.precio = precio;
            this.cantidad = cantidad;
            this.valor = valor;
            this.estado = estado;
            this.vendidas = vendidas;
            this.diasStock = diasStock;
            this.participacion = participacion;
            this.claseAbc = claseAbc;
        }

        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public String getCategoria() { return categoria; }
        public int getPrecio() { return precio; }
        public int getCantidad() { return cantidad; }
        public long getValor() { return valor; }
        public String getEstado() { return estado; }
        public int getVendidas() { return vendidas; }
        public Double getDiasStock() { return diasStock; }
        public double getParticipacion() { return participacion; }
        public String getClaseAbc() { return claseAbc; }
    }

    public static class ResumenCategoria {

        private final String categoria;
        private final int productos;
        private final int unidades;
        private final long valor;
        private final double participacion;

        public ResumenCategoria(String categoria, int productos, int unidades, long valor, double participacion) {
            this.categoria = categoria;
            this.productos = productos;
            this.unidades = unidades;
            this.valor = valor;
            this.participacion = participacion;
        }

        public String getCategoria() { return categoria; }
        public int getProductos() { return productos; }
        public int getUnidades() { return unidades; }
        public long getValor() { return valor; }
        public double getParticipacion() { return participacion; }
    }

    public static class ResumenAbc {

        private final String clase;
        private final int productos;
        private final long valor;
        private final double participacion;
        private final String criterio;

        public ResumenAbc(String clase, int productos, long valor, double participacion, String criterio) {
            this.clase = clase;
            this.productos = productos;
            this.valor = valor;
            this.participacion = participacion;
            this.criterio = criterio;
        }

        public String getClase() { return clase; }
        public int getProductos() { return productos; }
        public long getValor() { return valor; }
        public double getParticipacion() { return participacion; }
        public String getCriterio() { return criterio; }
    }

    public static class Reporte {

        private final List<FilaInventario> filas;
        private final String filtro;
        private final int productos;
        private final int unidades;
        private final long valor;
        private final int agotados;
        private final int stockBajo;
        private final int vendidas;
        private final List<ResumenCategoria> categorias;
        private final List<ResumenAbc> abc;

        public Reporte(List<FilaInventario> filas, String filtro, int productos, int unidades, long valor,
                       int agotados, int stockBajo, int vendidas,
                       List<ResumenCategoria> categorias, List<ResumenAbc> abc) {
            this.filas = filas;
            this.filtro = filtro;
            this.productos = productos;
            this.unidades = unidades;
            this.valor = valor;
            this.agotados = agotados;
            this.stockBajo = stockBajo;
            this.vendidas = vendidas;
            this.categorias = categorias;
            this.abc = abc;
        }

        public List<FilaInventario> getFilas() { return filas; }
        public String getFiltro() { return filtro; }
        public int getProductos() { return productos; }
        public int getUnidades() { return unidades; }
        public long getValor() { return valor; }
        public int getAgotados() { return agotados; }
        public int getStockBajo() { return stockBajo; }
        public int getVendidas() { return vendidas; }
        public List<ResumenCategoria> getCategorias() { return categorias; }
        public List<ResumenAbc> getAbc() { return abc; }
    }

    private int nz(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private long valorDe(Producto producto) {
        return (long) nz(producto.getPrecio()) * nz(producto.getCantidad());
    }

    private String estadoDe(int cantidad) {

        if (cantidad == 0) {
            return "Agotado";
        }

        if (cantidad <= 5) {
            return "Stock bajo";
        }

        return "Disponible";
    }

    private String describirFiltro(String texto, String categoria, String estado) {

        List<String> partes = new ArrayList<>();

        String buscado = texto == null ? "" : texto.trim();
        String categoriaElegida = categoria == null ? "" : categoria.trim();
        String estadoElegido = estado == null ? "" : estado.trim();

        if (!buscado.isEmpty()) {
            partes.add(buscado);
        }

        if (!categoriaElegida.isEmpty()) {
            partes.add("Categoría: " + categoriaElegida);
        }

        if ("agotado".equals(estadoElegido)) {
            partes.add("Estado: Agotados");
        } else if ("bajo".equals(estadoElegido)) {
            partes.add("Estado: Stock bajo (1 a 5)");
        } else if ("disponible".equals(estadoElegido)) {
            partes.add("Estado: Disponibles");
        }

        return partes.isEmpty() ? null : String.join(" · ", partes);
    }

    public Reporte generar(String texto, String categoria, String estado) {

        List<Producto> todos = productoRepository.findAll();

        long valorTotal = 0;
        for (Producto p : todos) {
            valorTotal += valorDe(p);
        }

        LocalDate hoy = LocalDate.now();
        LocalDateTime desde = hoy.minusDays(VENTANA_DIAS).atStartOfDay();

        Map<Integer, Long> vendidasPorProducto = new HashMap<>();
        for (Object[] fila : detalleVentaRepository.unidadesPorProductoDesde(desde)) {
            vendidasPorProducto.put(((Number) fila[0]).intValue(), ((Number) fila[1]).longValue());
        }

        long diasConDatos = 1;
        LocalDateTime primeraVenta = ventaRepository.primeraVentaDesde(desde);

        if (primeraVenta != null) {
            diasConDatos = ChronoUnit.DAYS.between(primeraVenta.toLocalDate(), hoy) + 1;
            diasConDatos = Math.max(1, Math.min(diasConDatos, VENTANA_DIAS));
        }

        List<Producto> porValor = new ArrayList<>(todos);
        porValor.sort(Comparator.comparingLong((Producto p) -> valorDe(p)).reversed()
                .thenComparing((Producto p) -> p.getNombre() == null ? "" : p.getNombre().toLowerCase()));

        Map<Integer, String> clases = new HashMap<>();
        double acumulado = 0;

        for (Producto p : porValor) {
            long valor = valorDe(p);
            String clase;

            if (valorTotal == 0 || valor == 0) {
                clase = "C";
            } else if (acumulado < LIMITE_A) {
                clase = "A";
            } else if (acumulado < LIMITE_B) {
                clase = "B";
            } else {
                clase = "C";
            }

            clases.put(p.getId(), clase);

            if (valorTotal > 0) {
                acumulado += (double) valor / valorTotal;
            }
        }

        String descripcionFiltro = describirFiltro(texto, categoria, estado);

        List<Producto> incluidos = new ArrayList<>(
                descripcionFiltro != null
                        ? productoConsultaService.buscarTodos(texto, categoria, estado)
                        : todos);

        incluidos.sort(Comparator.comparing(
                (Producto p) -> p.getNombre() == null ? "" : p.getNombre().toLowerCase()));

        List<FilaInventario> filas = new ArrayList<>();

        int unidades = 0;
        long valorIncluido = 0;
        int agotados = 0;
        int stockBajo = 0;
        int vendidasTotal = 0;

        for (Producto p : incluidos) {

            int cantidad = nz(p.getCantidad());
            int precio = nz(p.getPrecio());
            long valor = valorDe(p);
            int vendidas = vendidasPorProducto.getOrDefault(p.getId(), 0L).intValue();

            Double dias = null;
            if (vendidas > 0) {
                double promedio = (double) vendidas / diasConDatos;
                dias = cantidad / promedio;
            }

            double participacion = valorTotal > 0 ? (double) valor / valorTotal : 0;
            String estadoProducto = estadoDe(cantidad);

            filas.add(new FilaInventario(
                    p.getCodigo() == null ? "" : p.getCodigo(),
                    p.getNombre() == null ? "" : p.getNombre(),
                    p.getCategoria() == null ? "" : p.getCategoria(),
                    precio, cantidad, valor, estadoProducto, vendidas, dias, participacion,
                    clases.getOrDefault(p.getId(), "C")));

            unidades += cantidad;
            valorIncluido += valor;
            vendidasTotal += vendidas;

            if ("Agotado".equals(estadoProducto)) {
                agotados++;
            } else if ("Stock bajo".equals(estadoProducto)) {
                stockBajo++;
            }
        }

        Map<String, int[]> conteoCategorias = new LinkedHashMap<>();
        Map<String, Long> valorCategorias = new HashMap<>();

        for (FilaInventario f : filas) {
            String nombre = f.getCategoria().isEmpty() ? "Sin categoría" : f.getCategoria();
            int[] datos = conteoCategorias.computeIfAbsent(nombre, k -> new int[2]);
            datos[0] += 1;
            datos[1] += f.getCantidad();
            valorCategorias.merge(nombre, f.getValor(), Long::sum);
        }

        List<ResumenCategoria> categorias = new ArrayList<>();

        for (Map.Entry<String, int[]> e : conteoCategorias.entrySet()) {
            long valor = valorCategorias.getOrDefault(e.getKey(), 0L);
            double participacion = valorIncluido > 0 ? (double) valor / valorIncluido : 0;
            categorias.add(new ResumenCategoria(e.getKey(), e.getValue()[0], e.getValue()[1], valor, participacion));
        }

        categorias.sort(Comparator.comparingLong(ResumenCategoria::getValor).reversed());

        String[] letras = {"A", "B", "C"};
        String[] criterios = {
                "Concentran hasta el 80% del valor",
                "Siguiente 15% del valor",
                "El 5% restante del valor"
        };

        List<ResumenAbc> abc = new ArrayList<>();

        for (int i = 0; i < letras.length; i++) {
            int cuenta = 0;
            long valor = 0;

            for (FilaInventario f : filas) {
                if (letras[i].equals(f.getClaseAbc())) {
                    cuenta++;
                    valor += f.getValor();
                }
            }

            double participacion = valorIncluido > 0 ? (double) valor / valorIncluido : 0;
            abc.add(new ResumenAbc(letras[i], cuenta, valor, participacion, criterios[i]));
        }

        return new Reporte(filas, descripcionFiltro, filas.size(), unidades, valorIncluido,
                agotados, stockBajo, vendidasTotal, categorias, abc);
    }
}