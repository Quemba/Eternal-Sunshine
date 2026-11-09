package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.ConteoInventario;
import com.eternalsunshine.web.entity.DetalleConteo;
import com.eternalsunshine.web.entity.EstadoConteo;
import com.eternalsunshine.web.entity.EstadoLineaConteo;
import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.entity.Rol;
import com.eternalsunshine.web.entity.TipoDocumento;
import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.ConteoRepository;
import com.eternalsunshine.web.repository.DetalleConteoRepository;
import com.eternalsunshine.web.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ConteoService {

    private static final int CANTIDAD_MAXIMA = 100000;
    private static final long STOCK_MAXIMO = 1000000L;

    @Autowired
    private ConteoRepository conteoRepository;

    @Autowired
    private DetalleConteoRepository detalleRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private HistorialService historialService;

    @Autowired
    private NumeracionService numeracionService;

    public static class LineaConteoVista {

        private final Integer productoId;
        private final String codigo;
        private final String nombre;
        private final int cantidad;

        public LineaConteoVista(Integer productoId, String codigo, String nombre, int cantidad) {
            this.productoId = productoId;
            this.codigo = codigo;
            this.nombre = nombre;
            this.cantidad = cantidad;
        }

        public Integer getProductoId() { return productoId; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
    }

    public static class ResultadoConteo {

        private final List<LineaConteoVista> lineas;
        private final int totalProductos;
        private final int totalUnidades;

        public ResultadoConteo(List<LineaConteoVista> lineas, int totalProductos, int totalUnidades) {
            this.lineas = lineas;
            this.totalProductos = totalProductos;
            this.totalUnidades = totalUnidades;
        }

        public List<LineaConteoVista> getLineas() { return lineas; }
        public int getTotalProductos() { return totalProductos; }
        public int getTotalUnidades() { return totalUnidades; }
    }

    public static class FilaRevision {

        private final Integer id;
        private final String codigo;
        private final String nombre;
        private final int precio;
        private final int sistema;
        private final int contada;
        private final int diferencia;
        private final long valor;
        private final boolean conflicto;
        private final String estado;
        private final Integer stockAntes;
        private final Integer stockDespues;

        public FilaRevision(Integer id, String codigo, String nombre, int precio, int sistema, int contada,
                            int diferencia, long valor, boolean conflicto, String estado,
                            Integer stockAntes, Integer stockDespues) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
            this.precio = precio;
            this.sistema = sistema;
            this.contada = contada;
            this.diferencia = diferencia;
            this.valor = valor;
            this.conflicto = conflicto;
            this.estado = estado;
            this.stockAntes = stockAntes;
            this.stockDespues = stockDespues;
        }

        public Integer getId() { return id; }
        public String getCodigo() { return codigo; }
        public String getNombre() { return nombre; }
        public int getPrecio() { return precio; }
        public int getSistema() { return sistema; }
        public int getContada() { return contada; }
        public int getDiferencia() { return diferencia; }
        public long getValor() { return valor; }
        public boolean isConflicto() { return conflicto; }
        public String getEstado() { return estado; }
        public Integer getStockAntes() { return stockAntes; }
        public Integer getStockDespues() { return stockDespues; }
    }

    public static class ResumenConteo {

        private final int productos;
        private final int unidadesContadas;
        private final int coinciden;
        private final int conDiferencia;
        private final int unidadesFaltantes;
        private final int unidadesSobrantes;
        private final long valorFaltante;
        private final long valorSobrante;
        private final int aplicadas;

        public ResumenConteo(int productos, int unidadesContadas, int coinciden, int conDiferencia,
                             int unidadesFaltantes, int unidadesSobrantes,
                             long valorFaltante, long valorSobrante, int aplicadas) {
            this.productos = productos;
            this.unidadesContadas = unidadesContadas;
            this.coinciden = coinciden;
            this.conDiferencia = conDiferencia;
            this.unidadesFaltantes = unidadesFaltantes;
            this.unidadesSobrantes = unidadesSobrantes;
            this.valorFaltante = valorFaltante;
            this.valorSobrante = valorSobrante;
            this.aplicadas = aplicadas;
        }

        public int getProductos() { return productos; }
        public int getUnidadesContadas() { return unidadesContadas; }
        public int getCoinciden() { return coinciden; }
        public int getConDiferencia() { return conDiferencia; }
        public int getUnidadesFaltantes() { return unidadesFaltantes; }
        public int getUnidadesSobrantes() { return unidadesSobrantes; }
        public long getValorFaltante() { return valorFaltante; }
        public long getValorSobrante() { return valorSobrante; }
        public long getValorNeto() { return valorSobrante - valorFaltante; }
        public int getAplicadas() { return aplicadas; }
    }

    public static class DetalleConteoVista {

        private final List<FilaRevision> filas;
        private final ResumenConteo resumen;

        public DetalleConteoVista(List<FilaRevision> filas, ResumenConteo resumen) {
            this.filas = filas;
            this.resumen = resumen;
        }

        public List<FilaRevision> getFilas() { return filas; }
        public ResumenConteo getResumen() { return resumen; }
    }

    public static class ItemLista {

        private final Integer id;
        private final String numero;
        private final String nombre;
        private final String estadoTexto;
        private final String estadoClase;
        private final String usuario;
        private final LocalDateTime fecha;
        private final int productos;
        private final int unidades;
        private final Integer diferencias;
        private final boolean puedeContinuar;

        public ItemLista(Integer id, String numero, String nombre, String estadoTexto, String estadoClase,
                         String usuario, LocalDateTime fecha, int productos, int unidades,
                         Integer diferencias, boolean puedeContinuar) {
            this.id = id;
            this.numero = numero;
            this.nombre = nombre;
            this.estadoTexto = estadoTexto;
            this.estadoClase = estadoClase;
            this.usuario = usuario;
            this.fecha = fecha;
            this.productos = productos;
            this.unidades = unidades;
            this.diferencias = diferencias;
            this.puedeContinuar = puedeContinuar;
        }

        public Integer getId() { return id; }
        public String getNumero() { return numero; }
        public String getNombre() { return nombre; }
        public String getEstadoTexto() { return estadoTexto; }
        public String getEstadoClase() { return estadoClase; }
        public String getUsuario() { return usuario; }
        public LocalDateTime getFecha() { return fecha; }
        public int getProductos() { return productos; }
        public int getUnidades() { return unidades; }
        public Integer getDiferencias() { return diferencias; }
        public boolean isPuedeContinuar() { return puedeContinuar; }
    }

    public static class ResultadoRevision {

        private final int aplicadas;
        private final int sinAjustar;

        public ResultadoRevision(int aplicadas, int sinAjustar) {
            this.aplicadas = aplicadas;
            this.sinAjustar = sinAjustar;
        }

        public int getAplicadas() { return aplicadas; }
        public int getSinAjustar() { return sinAjustar; }
    }

    private int nz(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private String recortar(String texto) {
        return texto.length() > 250 ? texto.substring(0, 247) + "..." : texto;
    }

    private boolean esAdmin(Usuario usuario) {
        return usuario != null && Rol.ADMIN.equals(usuario.getRol());
    }

    private String conSigno(int numero) {
        return numero > 0 ? "+" + numero : String.valueOf(numero);
    }

    private String etiqueta(ConteoInventario conteo) {
        String numero = conteo.getNumero();
        return numero != null ? numero : "#" + conteo.getId();
    }

    private DetalleConteo buscarLinea(ConteoInventario conteo, Integer productoId) {

        for (DetalleConteo linea : conteo.getLineas()) {
            if (linea.getProducto() != null && linea.getProducto().getId().equals(productoId)) {
                return linea;
            }
        }

        return null;
    }

    private DetalleConteo nuevaLinea(ConteoInventario conteo, Producto producto) {

        DetalleConteo linea = new DetalleConteo();
        linea.setConteo(conteo);
        linea.setProducto(producto);
        linea.setCantidadSistema(nz(producto.getCantidad()));
        linea.setCantidadContada(0);
        linea.setFechaPrimerConteo(LocalDateTime.now());
        linea.setEstado(EstadoLineaConteo.PENDIENTE);

        conteo.getLineas().add(linea);

        return linea;
    }

    private ConteoInventario cargarEditable(Integer conteoId, Usuario usuario) {

        ConteoInventario conteo = conteoRepository.buscarParaActualizar(conteoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese conteo ya no existe."));

        if (conteo.getEstado() != EstadoConteo.EN_CURSO) {
            throw new IllegalArgumentException("Este conteo ya no está en curso y no se puede modificar.");
        }

        if (conteo.getUsuario() == null || usuario == null
                || !conteo.getUsuario().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Solo quien inició el conteo puede modificarlo.");
        }

        return conteo;
    }

    @Transactional(readOnly = true)
    public ResultadoConteo resultado(ConteoInventario conteo) {

        List<LineaConteoVista> lista = new ArrayList<>();
        int unidades = 0;

        for (DetalleConteo linea : conteo.getLineas()) {

            Producto producto = linea.getProducto();

            if (producto == null) {
                continue;
            }

            int cantidad = nz(linea.getCantidadContada());

            lista.add(new LineaConteoVista(
                    producto.getId(),
                    producto.getCodigo() == null ? "" : producto.getCodigo(),
                    producto.getNombre() == null ? "" : producto.getNombre(),
                    cantidad));

            unidades += cantidad;
        }

        return new ResultadoConteo(lista, lista.size(), unidades);
    }

    @Transactional
    public ConteoInventario iniciar(String nombre, Usuario usuario) {

        String limpio = nombre == null ? "" : nombre.trim().replaceAll("\\s+", " ");

        if (limpio.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede superar 100 caracteres.");
        }

        ConteoInventario conteo = new ConteoInventario();
        conteo.setNombre(limpio.isEmpty() ? null : limpio);
        conteo.setEstado(EstadoConteo.EN_CURSO);
        conteo.setUsuario(usuario);
        conteo.setFechaInicio(LocalDateTime.now());

        conteo = conteoRepository.save(conteo);

        historialService.registrar(usuario,
                recortar("INICIÓ CONTEO" + (limpio.isEmpty() ? "" : ": " + limpio)));

        return conteo;
    }

    @Transactional
    public ResultadoConteo contar(Integer conteoId, Integer productoId, Integer delta, Usuario usuario) {

        if (productoId == null || delta == null || delta == 0 || Math.abs(delta) > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad no es válida.");
        }

        ConteoInventario conteo = cargarEditable(conteoId, usuario);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese producto ya no existe."));

        DetalleConteo linea = buscarLinea(conteo, productoId);

        if (linea == null) {
            if (delta < 0) {
                throw new IllegalArgumentException("Ese producto todavía no está en el conteo.");
            }

            linea = nuevaLinea(conteo, producto);
        }

        int nueva = nz(linea.getCantidadContada()) + delta;

        if (nueva < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }

        if (nueva > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad máxima por producto es 100.000.");
        }

        linea.setCantidadContada(nueva);

        conteoRepository.save(conteo);

        return resultado(conteo);
    }

    @Transactional
    public ResultadoConteo fijar(Integer conteoId, Integer productoId, Integer cantidad, Usuario usuario) {

        if (productoId == null || cantidad == null || cantidad < 0 || cantidad > CANTIDAD_MAXIMA) {
            throw new IllegalArgumentException("La cantidad debe ser un número entre 0 y 100.000.");
        }

        ConteoInventario conteo = cargarEditable(conteoId, usuario);

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese producto ya no existe."));

        DetalleConteo linea = buscarLinea(conteo, productoId);

        if (linea == null) {
            linea = nuevaLinea(conteo, producto);
        }

        linea.setCantidadContada(cantidad);

        conteoRepository.save(conteo);

        return resultado(conteo);
    }

    @Transactional
    public ResultadoConteo quitar(Integer conteoId, Integer productoId, Usuario usuario) {

        ConteoInventario conteo = cargarEditable(conteoId, usuario);

        conteo.getLineas().removeIf(linea ->
                linea.getProducto() != null && linea.getProducto().getId().equals(productoId));

        conteoRepository.save(conteo);

        return resultado(conteo);
    }

    @Transactional
    public void enviar(Integer conteoId, Usuario usuario) {

        ConteoInventario conteo = cargarEditable(conteoId, usuario);

        if (conteo.getLineas().isEmpty()) {
            throw new IllegalArgumentException("Cuenta al menos un producto antes de enviar el conteo.");
        }

        LocalDateTime ahora = LocalDateTime.now();

        conteo.setEstado(EstadoConteo.PENDIENTE_REVISION);
        conteo.setFechaEnvio(ahora);
        conteo.setPeriodo(NumeracionService.periodo(ahora));
        conteo.setConsecutivo(numeracionService.siguiente(TipoDocumento.CONTEO, ahora));
        conteoRepository.save(conteo);

        historialService.registrar(usuario,
                "ENVIÓ CONTEO " + conteo.getNumero() + " A REVISIÓN (" + conteo.getLineas().size()
                        + (conteo.getLineas().size() == 1 ? " producto)" : " productos)"));
    }

    @Transactional
    public void cancelar(Integer conteoId, Usuario usuario) {

        ConteoInventario conteo = conteoRepository.buscarParaActualizar(conteoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese conteo ya no existe."));

        if (conteo.getEstado() != EstadoConteo.EN_CURSO) {
            throw new IllegalArgumentException("Solo se puede cancelar un conteo que sigue en curso.");
        }

        boolean propio = conteo.getUsuario() != null && usuario != null
                && conteo.getUsuario().getId().equals(usuario.getId());

        if (!propio && !esAdmin(usuario)) {
            throw new IllegalArgumentException("No puedes cancelar el conteo de otra persona.");
        }

        conteo.setEstado(EstadoConteo.CANCELADO);
        conteo.setFechaCancelacion(LocalDateTime.now());
        conteo.setUsuarioCancela(usuario);
        conteo.getLineas().clear();

        conteoRepository.save(conteo);

        String iniciado = conteo.getFechaInicio() == null
                ? ""
                : " (iniciado el " + conteo.getFechaInicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + ")";

        historialService.registrar(usuario,
                recortar("CANCELÓ CONTEO" + (conteo.getNombre() == null ? "" : " «" + conteo.getNombre() + "»") + iniciado));
    }

    @Transactional
    public ResultadoRevision aplicarRevision(Integer conteoId, Collection<Integer> idsLineas, Usuario admin) {

        if (!esAdmin(admin)) {
            throw new IllegalArgumentException("Solo el administrador puede revisar un conteo.");
        }

        ConteoInventario conteo = conteoRepository.buscarParaActualizar(conteoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese conteo ya no existe."));

        if (conteo.getEstado() != EstadoConteo.PENDIENTE_REVISION) {
            throw new IllegalArgumentException("Este conteo ya fue revisado o todavía no ha sido enviado.");
        }

        Set<Integer> elegidas = idsLineas == null ? new HashSet<>() : new HashSet<>(idsLineas);

        List<DetalleConteo> aplicar = new ArrayList<>();

        for (DetalleConteo linea : conteo.getLineas()) {
            int diferencia = nz(linea.getCantidadContada()) - nz(linea.getCantidadSistema());

            if (diferencia != 0 && linea.getProducto() != null && elegidas.contains(linea.getId())) {
                aplicar.add(linea);
            }
        }

        aplicar.sort(Comparator.comparing((DetalleConteo l) -> l.getProducto().getId()));

        Map<Integer, Producto> productos = new HashMap<>();
        Map<Integer, Integer> actuales = new HashMap<>();

        for (DetalleConteo linea : aplicar) {

            Producto producto = productoRepository.buscarParaActualizar(linea.getProducto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no existe."));

            int actual = nz(producto.getCantidad());
            int diferencia = nz(linea.getCantidadContada()) - nz(linea.getCantidadSistema());
            long nuevo = (long) actual + diferencia;

            if (nuevo < 0) {
                throw new IllegalArgumentException("No se puede ajustar «" + producto.getNombre()
                        + "»: el stock quedaría negativo (actual: " + actual + ", ajuste: " + conSigno(diferencia)
                        + "). Desmárcalo e inténtalo de nuevo.");
            }

            if (nuevo > STOCK_MAXIMO) {
                throw new IllegalArgumentException("El stock de «" + producto.getNombre()
                        + "» superaría el máximo permitido (1.000.000). Desmárcalo e inténtalo de nuevo.");
            }

            productos.put(linea.getId(), producto);
            actuales.put(linea.getId(), actual);
        }

        LocalDateTime ahora = LocalDateTime.now();

        for (DetalleConteo linea : aplicar) {

            Producto producto = productos.get(linea.getId());
            int antes = actuales.get(linea.getId());
            int diferencia = nz(linea.getCantidadContada()) - nz(linea.getCantidadSistema());
            int despues = antes + diferencia;

            producto.setCantidad(despues);
            productoRepository.save(producto);

            linea.setStockAntesAjuste(antes);
            linea.setStockDespuesAjuste(despues);
            linea.setEstado(EstadoLineaConteo.APLICADA);
            linea.setFechaAplicacion(ahora);

            historialService.registrar(admin,
                    recortar("CORRIGIÓ STOCK de " + producto.getNombre() + ": " + antes + " → " + despues
                            + " (conteo " + etiqueta(conteo) + ")"));
        }

        int sinAjustar = 0;

        for (DetalleConteo linea : conteo.getLineas()) {

            if (linea.getEstado() != EstadoLineaConteo.APLICADA) {

                linea.setEstado(EstadoLineaConteo.DESCARTADA);

                if (nz(linea.getCantidadContada()) != nz(linea.getCantidadSistema())) {
                    sinAjustar++;
                }
            }
        }

        conteo.setEstado(EstadoConteo.REVISADO);
        conteo.setFechaRevision(ahora);
        conteo.setUsuarioRevisor(admin);
        conteoRepository.save(conteo);

        historialService.registrar(admin,
                "REVISÓ CONTEO " + etiqueta(conteo) + ": " + aplicar.size() + " ajustes aplicados, "
                        + sinAjustar + " sin ajustar");

        return new ResultadoRevision(aplicar.size(), sinAjustar);
    }

    @Transactional(readOnly = true)
    public DetalleConteoVista detalle(ConteoInventario conteo) {

        boolean pendiente = conteo.getEstado() == EstadoConteo.PENDIENTE_REVISION;

        List<FilaRevision> filas = new ArrayList<>();

        int unidadesContadas = 0;
        int coinciden = 0;
        int conDiferencia = 0;
        int faltantes = 0;
        int sobrantes = 0;
        int aplicadas = 0;
        long valorFaltante = 0;
        long valorSobrante = 0;

        for (DetalleConteo linea : conteo.getLineas()) {

            Producto producto = linea.getProducto();

            int sistema = nz(linea.getCantidadSistema());
            int contada = nz(linea.getCantidadContada());
            int diferencia = contada - sistema;
            int precio = producto == null ? 0 : nz(producto.getPrecio());
            long valor = (long) diferencia * precio;

            boolean conflicto = false;

            if (pendiente && diferencia != 0 && producto != null && linea.getFechaPrimerConteo() != null) {
                conflicto = detalleRepository.contarAplicadasDespuesDe(
                        producto.getId(),
                        EstadoLineaConteo.APLICADA,
                        linea.getFechaPrimerConteo(),
                        conteo.getId()) > 0;
            }

            filas.add(new FilaRevision(
                    linea.getId(),
                    producto == null || producto.getCodigo() == null ? "" : producto.getCodigo(),
                    producto == null || producto.getNombre() == null ? "(producto eliminado)" : producto.getNombre(),
                    precio,
                    sistema,
                    contada,
                    diferencia,
                    valor,
                    conflicto,
                    linea.getEstado() == null ? "PENDIENTE" : linea.getEstado().name(),
                    linea.getStockAntesAjuste(),
                    linea.getStockDespuesAjuste()));

            unidadesContadas += contada;

            if (diferencia == 0) {
                coinciden++;
            } else {
                conDiferencia++;

                if (diferencia < 0) {
                    faltantes += -diferencia;
                    valorFaltante += -valor;
                } else {
                    sobrantes += diferencia;
                    valorSobrante += valor;
                }
            }

            if (linea.getEstado() == EstadoLineaConteo.APLICADA) {
                aplicadas++;
            }
        }

        return new DetalleConteoVista(filas, new ResumenConteo(
                filas.size(), unidadesContadas, coinciden, conDiferencia,
                faltantes, sobrantes, valorFaltante, valorSobrante, aplicadas));
    }

    @Transactional(readOnly = true)
    public List<ItemLista> listar(Usuario usuario, String filtro) {

        boolean admin = esAdmin(usuario);

        EstadoConteo estado = null;

        if ("en_curso".equals(filtro)) {
            estado = EstadoConteo.EN_CURSO;
        } else if ("por_revisar".equals(filtro)) {
            estado = EstadoConteo.PENDIENTE_REVISION;
        } else if ("revisados".equals(filtro)) {
            estado = EstadoConteo.REVISADO;
        } else if ("cancelados".equals(filtro)) {
            estado = EstadoConteo.CANCELADO;
        }

        List<ConteoInventario> conteos;

        if (admin) {
            conteos = estado == null
                    ? conteoRepository.findTop100ByOrderByFechaInicioDescIdDesc()
                    : conteoRepository.findTop100ByEstadoOrderByFechaInicioDescIdDesc(estado);
        } else {
            conteos = estado == null
                    ? conteoRepository.findTop100ByUsuario_IdOrderByFechaInicioDescIdDesc(usuario.getId())
                    : conteoRepository.findTop100ByUsuario_IdAndEstadoOrderByFechaInicioDescIdDesc(usuario.getId(), estado);
        }

        List<ItemLista> items = new ArrayList<>();

        for (ConteoInventario conteo : conteos) {

            int unidades = 0;
            int diferencias = 0;

            for (DetalleConteo linea : conteo.getLineas()) {
                unidades += nz(linea.getCantidadContada());

                if (nz(linea.getCantidadContada()) != nz(linea.getCantidadSistema())) {
                    diferencias++;
                }
            }

            boolean muestraDiferencias = admin
                    ? (conteo.getEstado() == EstadoConteo.PENDIENTE_REVISION || conteo.getEstado() == EstadoConteo.REVISADO)
                    : conteo.getEstado() == EstadoConteo.REVISADO;

            boolean propio = conteo.getUsuario() != null
                    && conteo.getUsuario().getId().equals(usuario.getId());

            items.add(new ItemLista(
                    conteo.getId(),
                    conteo.getNumero(),
                    conteo.getNombre() == null ? "Sin nombre" : conteo.getNombre(),
                    conteo.getEstadoTexto(),
                    conteo.getEstadoClase(),
                    conteo.getUsuario() == null ? "-" : conteo.getUsuario().getUsername(),
                    conteo.getFechaInicio(),
                    conteo.getLineas().size(),
                    unidades,
                    muestraDiferencias ? diferencias : null,
                    conteo.getEstado() == EstadoConteo.EN_CURSO && propio));
        }

        return items;
    }
}