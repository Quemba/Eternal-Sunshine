package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Historial;
import com.eternalsunshine.web.repository.HistorialBusquedaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Service
public class HistorialConsultaService {

    public static final int TAMANO_PAGINA = 20;

    @Autowired
    private HistorialBusquedaRepository repositorio;

    public static class Fila {

        private final Integer id;
        private final LocalDateTime fecha;
        private final String usuario;
        private final String accion;
        private final String tipoClave;
        private final String tipoEtiqueta;
        private final String tipoIcono;

        public Fila(Integer id, LocalDateTime fecha, String usuario, String accion, TipoAccionHistorial tipo) {
            this.id = id;
            this.fecha = fecha;
            this.usuario = usuario;
            this.accion = accion;
            this.tipoClave = tipo.getClave();
            this.tipoEtiqueta = tipo.getEtiqueta();
            this.tipoIcono = tipo.getIcono();
        }

        public Integer getId() { return id; }
        public LocalDateTime getFecha() { return fecha; }
        public String getUsuario() { return usuario; }
        public String getAccion() { return accion; }
        public String getTipoClave() { return tipoClave; }
        public String getTipoEtiqueta() { return tipoEtiqueta; }
        public String getTipoIcono() { return tipoIcono; }
    }

    public static class ItemPagina {

        private final int numero;
        private final boolean puntos;
        private final boolean actual;

        public ItemPagina(int numero, boolean puntos, boolean actual) {
            this.numero = numero;
            this.puntos = puntos;
            this.actual = actual;
        }

        public int getNumero() { return numero; }
        public boolean isPuntos() { return puntos; }
        public boolean isActual() { return actual; }
    }

    public static class Resultado {

        private final List<Fila> filas;
        private final int pagina;
        private final int totalPaginas;
        private final long total;
        private final int desdeRegistro;
        private final int hastaRegistro;
        private final List<ItemPagina> items;

        public Resultado(List<Fila> filas, int pagina, int totalPaginas, long total,
                         int desdeRegistro, int hastaRegistro, List<ItemPagina> items) {
            this.filas = filas;
            this.pagina = pagina;
            this.totalPaginas = totalPaginas;
            this.total = total;
            this.desdeRegistro = desdeRegistro;
            this.hastaRegistro = hastaRegistro;
            this.items = items;
        }

        public List<Fila> getFilas() { return filas; }
        public int getPagina() { return pagina; }
        public int getTotalPaginas() { return totalPaginas; }
        public long getTotal() { return total; }
        public int getDesdeRegistro() { return desdeRegistro; }
        public int getHastaRegistro() { return hastaRegistro; }
        public List<ItemPagina> getItems() { return items; }
    }

    private String escaparLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private Specification<Historial> construirFiltro(
            String texto,
            Integer usuarioId,
            String tipo,
            LocalDate desde,
            LocalDate hasta) {

        return (root, query, cb) -> {

            List<Predicate> condiciones = new ArrayList<>();

            String limpio = texto == null ? "" : texto.trim();

            if (!limpio.isEmpty()) {
                String patron = "%" + escaparLike(limpio.toLowerCase()) + "%";
                Join<Object, Object> usuario = root.join("usuario", JoinType.LEFT);

                condiciones.add(cb.or(
                        cb.like(cb.lower(root.<String>get("accion")), patron, '\\'),
                        cb.like(cb.lower(usuario.<String>get("username")), patron, '\\')));
            }

            if (usuarioId != null) {
                condiciones.add(cb.equal(root.get("usuario").get("id"), usuarioId));
            }

            TipoAccionHistorial tipoElegido = TipoAccionHistorial.porClave(tipo);

            if (tipoElegido != null && tipoElegido != TipoAccionHistorial.OTRO) {
                List<Predicate> alternativas = new ArrayList<>();

                for (String prefijo : tipoElegido.getPrefijos()) {
                    alternativas.add(cb.like(root.<String>get("accion"), escaparLike(prefijo) + "%", '\\'));
                }

                condiciones.add(cb.or(alternativas.toArray(new Predicate[0])));
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
    }

    private List<ItemPagina> construirItems(int actual, int total) {

        TreeSet<Integer> visibles = new TreeSet<>();

        visibles.add(1);
        visibles.add(total);

        for (int n = actual - 2; n <= actual + 2; n++) {
            if (n >= 1 && n <= total) {
                visibles.add(n);
            }
        }

        List<ItemPagina> items = new ArrayList<>();
        int anterior = 0;

        for (Integer numero : visibles) {
            if (anterior != 0 && numero - anterior > 1) {
                items.add(new ItemPagina(0, true, false));
            }
            items.add(new ItemPagina(numero, false, numero == actual));
            anterior = numero;
        }

        return items;
    }

    public Resultado buscar(
            String texto,
            Integer usuarioId,
            String tipo,
            LocalDate desde,
            LocalDate hasta,
            int pagina) {

        Specification<Historial> filtro = construirFiltro(texto, usuarioId, tipo, desde, hasta);
        Sort orden = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));

        int actual = Math.max(1, pagina);

        Page<Historial> resultado = repositorio.findAll(
                filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));

        if (resultado.getTotalPages() > 0 && actual > resultado.getTotalPages()) {
            actual = resultado.getTotalPages();
            resultado = repositorio.findAll(
                    filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));
        }

        List<Fila> filas = new ArrayList<>();

        for (Historial h : resultado.getContent()) {
            filas.add(new Fila(
                    h.getId(),
                    h.getFecha(),
                    h.getUsuario() != null ? h.getUsuario().getUsername() : "-",
                    h.getAccion(),
                    TipoAccionHistorial.detectar(h.getAccion())));
        }

        int totalPaginas = resultado.getTotalPages();
        int desdeRegistro = filas.isEmpty() ? 0 : (actual - 1) * TAMANO_PAGINA + 1;
        int hastaRegistro = filas.isEmpty() ? 0 : desdeRegistro + filas.size() - 1;

        return new Resultado(
                filas,
                actual,
                totalPaginas,
                resultado.getTotalElements(),
                desdeRegistro,
                hastaRegistro,
                totalPaginas > 0 ? construirItems(actual, totalPaginas) : new ArrayList<>());
    }
}