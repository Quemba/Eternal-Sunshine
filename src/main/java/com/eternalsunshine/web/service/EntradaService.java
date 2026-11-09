package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.EntradaMercancia;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.EntradaMercanciaRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EntradaService {

    public static final int TAMANO_PAGINA = 20;

    private static final int CANTIDAD_MAXIMA = 100000;
    private static final long STOCK_MAXIMO = 1000000L;

    private static final Pattern CODIGO_ENTRADA =
            Pattern.compile("^ENT-(\\d{4})-(\\d{2})-(\\d{1,6})$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private EntradaMercanciaRepository entradaRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private NumeracionService numeracionService;

    public static class LineaEntrada {
        private Integer productoId;
        private Integer cantidad;

        public Integer getProductoId() { return productoId; }
        public void setProductoId(Integer productoId) { this.productoId = productoId; }
        public Integer getCantidad() { return cantidad; }
        public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
    }

    public static class FacturaDuplicadaException extends RuntimeException {
        public FacturaDuplicadaException(String mensaje) {
            super(mensaje);
        }
    }

    private String recortar(String texto) {
        return texto.length() > 250 ? texto.substring(0, 247) + "..." : texto;
    }

    private String escaparLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private String limpiar(String texto) {
        return texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
    }

    private String proveedorCanonico(String proveedor) {
        for (String existente : entradaRepository.proveedoresDistintos()) {
            if (existente.equalsIgnoreCase(proveedor)) {
                return existente;
            }
        }

        return proveedor;
    }

    public List<String> proveedores() {
        return entradaRepository.proveedoresDistintos();
    }

    public String numeroDeLaUltimaEntrada(Integer usuarioId) {

        if (usuarioId == null) {
            return null;
        }

        return entradaRepository.findFirstByUsuario_IdOrderByFechaDescIdDesc(usuarioId)
                .map(EntradaMercancia::getNumero)
                .filter(numero -> !"-".equals(numero))
                .orElse(null);
    }

    public PaginaVista<EntradaMercancia> buscar(
            String texto,
            String proveedor,
            String estado,
            LocalDate desde,
            LocalDate hasta,
            int pagina) {

        String buscado = texto == null ? "" : texto.trim();
        String proveedorElegido = proveedor == null ? "" : proveedor.trim();
        String estadoElegido = estado == null ? "" : estado.trim();

        Specification<EntradaMercancia> filtro = (root, query, cb) -> {

            List<Predicate> condiciones = new ArrayList<>();

            if (!buscado.isEmpty()) {

                Matcher codigo = CODIGO_ENTRADA.matcher(buscado);

                if (codigo.matches()) {
                    condiciones.add(cb.and(
                            cb.equal(root.get("periodo"), codigo.group(1) + "-" + codigo.group(2)),
                            cb.equal(root.get("consecutivo"), Integer.parseInt(codigo.group(3)))));

                } else {
                    String patron = "%" + escaparLike(buscado.toLowerCase()) + "%";
                    Join<Object, Object> producto = root.join("producto", JoinType.LEFT);

                    List<Predicate> alternativas = new ArrayList<>();

                    alternativas.add(cb.like(cb.lower(cb.coalesce(root.<String>get("proveedor"), "")), patron, '\\'));
                    alternativas.add(cb.like(cb.lower(cb.coalesce(root.<String>get("numeroFactura"), "")), patron, '\\'));
                    alternativas.add(cb.like(cb.lower(cb.coalesce(root.<String>get("nota"), "")), patron, '\\'));
                    alternativas.add(cb.like(cb.lower(cb.coalesce(producto.<String>get("nombre"), "")), patron, '\\'));

                    String soloNumero = buscado.startsWith("#") ? buscado.substring(1).trim() : buscado;

                    if (soloNumero.matches("\\d{1,9}")) {
                        alternativas.add(cb.equal(root.get("consecutivo"), Integer.parseInt(soloNumero)));
                    }

                    condiciones.add(cb.or(alternativas.toArray(new Predicate[0])));
                }
            }

            if (!proveedorElegido.isEmpty()) {
                condiciones.add(cb.equal(
                        cb.lower(root.<String>get("proveedor")), proveedorElegido.toLowerCase()));
            }

            if ("activa".equals(estadoElegido)) {
                condiciones.add(cb.or(
                        cb.isNull(root.get("anulada")),
                        cb.isFalse(root.<Boolean>get("anulada"))));
            } else if ("anulada".equals(estadoElegido)) {
                condiciones.add(cb.isTrue(root.<Boolean>get("anulada")));
            }

            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("fecha"), desde.atStartOfDay()));
            }

            if (hasta != null) {
                condiciones.add(cb.lessThan(
                        root.<LocalDateTime>get("fecha"), hasta.plusDays(1).atStartOfDay()));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        Sort orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
        int actual = Math.max(1, pagina);

        Page<EntradaMercancia> resultado = entradaRepository.findAll(
                filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));

        if (resultado.getTotalPages() > 0 && actual > resultado.getTotalPages()) {
            actual = resultado.getTotalPages();
            resultado = entradaRepository.findAll(
                    filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));
        }

        return new PaginaVista<>(
                resultado.getContent(),
                actual,
                TAMANO_PAGINA,
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }

    @Transactional
    public List<EntradaMercancia> registrar(
            List<LineaEntrada> lineas,
            String nota,
            String proveedor,
            String numeroFactura,
            LocalDate fechaFactura,
            boolean confirmarDuplicada,
            Usuario usuario) {

        if (lineas == null || lineas.isEmpty()) {
            throw new IllegalArgumentException("No hay productos para registrar.");
        }

        String notaLimpia = limpiar(nota);

        if (notaLimpia.length() > 200) {
            throw new IllegalArgumentException("La nota no puede superar 200 caracteres.");
        }

        String proveedorLimpio = limpiar(proveedor);

        if (proveedorLimpio.isEmpty()) {
            throw new IllegalArgumentException("Escribe el proveedor.");
        }

        if (proveedorLimpio.length() > 100) {
            throw new IllegalArgumentException("El proveedor no puede superar 100 caracteres.");
        }

        String facturaLimpia = limpiar(numeroFactura);

        if (facturaLimpia.length() > 50) {
            throw new IllegalArgumentException("El número de factura no puede superar 50 caracteres.");
        }

        if (fechaFactura != null && fechaFactura.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la factura no puede ser futura.");
        }

        proveedorLimpio = proveedorCanonico(proveedorLimpio);

        if (!facturaLimpia.isEmpty() && !confirmarDuplicada) {
            List<EntradaMercancia> previas =
                    entradaRepository.buscarFacturaRegistrada(proveedorLimpio, facturaLimpia);

            if (!previas.isEmpty()) {
                EntradaMercancia primera = previas.get(0);

                String quien = primera.getUsuario() != null
                        ? primera.getUsuario().getUsername()
                        : "otro usuario";

                String cuando = primera.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                throw new FacturaDuplicadaException(
                        "La factura " + facturaLimpia + " de «" + proveedorLimpio + "» ya se registró el "
                                + cuando + " por " + quien + " (" + previas.size()
                                + (previas.size() == 1 ? " producto" : " productos") + "). "
                                + "Si es una entrega parcial de la misma factura, puedes registrarla de todos modos.");
            }
        }

        Map<Integer, Integer> cantidades = new LinkedHashMap<>();

        for (LineaEntrada linea : lineas) {
            if (linea == null || linea.getProductoId() == null || linea.getCantidad() == null
                    || linea.getCantidad() <= 0 || linea.getCantidad() > CANTIDAD_MAXIMA) {
                throw new IllegalArgumentException("Hay una cantidad inválida: debe ser un número entre 1 y 100.000.");
            }
            cantidades.merge(linea.getProductoId(), linea.getCantidad(), Integer::sum);
        }

        for (Integer total : cantidades.values()) {
            if (total > CANTIDAD_MAXIMA) {
                throw new IllegalArgumentException("La cantidad por producto no puede superar 100.000.");
            }
        }

        Map<Integer, Producto> bloqueados = new HashMap<>();

        for (Integer productoId : new TreeSet<>(cantidades.keySet())) {
            Producto bloqueado = productoRepository.buscarParaActualizar(productoId)
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no existe."));
            bloqueados.put(productoId, bloqueado);
        }

        LocalDateTime ahora = LocalDateTime.now();
        String periodoEntrada = NumeracionService.periodo(ahora);
        int consecutivo = numeracionService.siguiente(TipoDocumento.ENTRADA, ahora);
        String codigoEntrada = TipoDocumento.ENTRADA.codigo(periodoEntrada, consecutivo);

        LocalDate fechaGuardada = facturaLimpia.isEmpty() ? null : fechaFactura;
        List<EntradaMercancia> creadas = new ArrayList<>();
        StringBuilder resumen = new StringBuilder("ENTRADA DE MERCANCÍA " + codigoEntrada + ": ");
        boolean primero = true;

        for (Map.Entry<Integer, Integer> entrada : cantidades.entrySet()) {

            Producto producto = bloqueados.get(entrada.getKey());
            int cantidad = entrada.getValue();
            int antes = producto.getCantidad() == null ? 0 : producto.getCantidad();
            long despues = (long) antes + cantidad;

            if (despues > STOCK_MAXIMO) {
                throw new IllegalArgumentException("El stock de " + producto.getNombre()
                        + " superaría el máximo permitido (1.000.000).");
            }

            producto.setCantidad((int) despues);
            productoRepository.save(producto);

            EntradaMercancia registro = new EntradaMercancia();
            registro.setProducto(producto);
            registro.setCantidad(cantidad);
            registro.setStockAntes(antes);
            registro.setStockDespues((int) despues);
            registro.setNota(notaLimpia.isEmpty() ? null : notaLimpia);
            registro.setProveedor(proveedorLimpio);
            registro.setNumeroFactura(facturaLimpia.isEmpty() ? null : facturaLimpia);
            registro.setFechaFactura(fechaGuardada);
            registro.setPeriodo(periodoEntrada);
            registro.setConsecutivo(consecutivo);
            registro.setUsuario(usuario);
            registro.setFecha(ahora);
            registro.setAnulada(false);

            creadas.add(entradaRepository.save(registro));

            if (!primero) {
                resumen.append(", ");
            }
            resumen.append("+").append(cantidad).append(" ").append(producto.getNombre());
            primero = false;
        }

        resumen.append(" · Proveedor: ").append(proveedorLimpio);

        if (!facturaLimpia.isEmpty()) {
            resumen.append(" · Factura: ").append(facturaLimpia);
        }

        if (!notaLimpia.isEmpty()) {
            resumen.append(" · Nota: ").append(notaLimpia);
        }

        historialService.registrar(usuario, recortar(resumen.toString()));

        return creadas;
    }

    @Transactional
    public void anular(Integer entradaId, Usuario admin) {

        EntradaMercancia entrada = entradaRepository.buscarParaActualizar(entradaId)
                .orElseThrow(() -> new IllegalArgumentException("Esa entrada ya no existe."));

        if (Boolean.TRUE.equals(entrada.getAnulada())) {
            throw new IllegalArgumentException("Esa entrada ya fue anulada.");
        }

        if (entrada.getProducto() == null) {
            throw new IllegalArgumentException("El producto de esa entrada ya no existe.");
        }

        Producto producto = productoRepository.buscarParaActualizar(entrada.getProducto().getId())
                .orElseThrow(() -> new IllegalArgumentException("El producto de esa entrada ya no existe."));

        int actual = producto.getCantidad() == null ? 0 : producto.getCantidad();
        int cantidad = entrada.getCantidad();

        if (actual < cantidad) {
            throw new IllegalArgumentException("No se puede anular: de esa mercancía ya se vendió o se retiró una parte "
                    + "(stock actual: " + actual + ", la entrada fue de " + cantidad + ").");
        }

        producto.setCantidad(actual - cantidad);
        productoRepository.save(producto);

        entrada.setAnulada(true);
        entrada.setUsuarioAnula(admin);
        entrada.setFechaAnulacion(LocalDateTime.now());
        entradaRepository.save(entrada);

        historialService.registrar(admin,
                recortar("ANULÓ ENTRADA " + entrada.getNumero() + ": -" + cantidad + " " + producto.getNombre()));
    }
}