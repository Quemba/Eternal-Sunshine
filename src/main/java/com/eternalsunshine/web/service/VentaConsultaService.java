package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Venta;
import com.eternalsunshine.web.repository.VentaBusquedaRepository;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VentaConsultaService {

    public static final int TAMANO_PAGINA = 20;

    private static final Pattern CODIGO_VENTA =
            Pattern.compile("^VEN-(\\d{4})-(\\d{2})-(\\d{1,6})$", Pattern.CASE_INSENSITIVE);

    @Autowired
    private VentaBusquedaRepository repositorio;

    private String escaparLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public PaginaVista<Venta> buscar(
            String texto,
            Integer vendedorId,
            String estado,
            LocalDate desde,
            LocalDate hasta,
            int pagina) {

        String buscado = texto == null ? "" : texto.trim();
        String estadoElegido = estado == null ? "" : estado.trim();

        Specification<Venta> filtro = (root, query, cb) -> {

            List<Predicate> condiciones = new ArrayList<>();

            if (!buscado.isEmpty()) {

                String numero = buscado.startsWith("#") ? buscado.substring(1).trim() : buscado;
                Matcher codigo = CODIGO_VENTA.matcher(buscado);

                if (codigo.matches()) {
                    condiciones.add(cb.and(
                            cb.equal(root.get("periodo"), codigo.group(1) + "-" + codigo.group(2)),
                            cb.equal(root.get("consecutivo"), Integer.parseInt(codigo.group(3)))));

                } else if (numero.matches("\\d{1,9}")) {
                    condiciones.add(cb.equal(root.get("consecutivo"), Integer.parseInt(numero)));

                } else {
                    String patron = "%" + escaparLike(buscado.toLowerCase()) + "%";

                    Join<Object, Object> detalle = root.join("detalles", JoinType.LEFT);
                    Join<Object, Object> producto = detalle.join("producto", JoinType.LEFT);

                    condiciones.add(cb.or(
                            cb.like(cb.lower(cb.coalesce(producto.<String>get("nombre"), "")), patron, '\\'),
                            cb.like(cb.lower(cb.coalesce(producto.<String>get("codigo"), "")), patron, '\\')));

                    query.distinct(true);
                }
            }

            if (vendedorId != null) {
                condiciones.add(cb.equal(root.get("usuario").get("id"), vendedorId));
            }

            if ("vigente".equals(estadoElegido)) {
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

        Page<Venta> resultado = repositorio.findAll(
                filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));

        if (resultado.getTotalPages() > 0 && actual > resultado.getTotalPages()) {
            actual = resultado.getTotalPages();
            resultado = repositorio.findAll(
                    filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, orden));
        }

        return new PaginaVista<>(
                resultado.getContent(),
                actual,
                TAMANO_PAGINA,
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }
}