package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Producto;
import com.eternalsunshine.web.repository.ProductoBusquedaRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductoConsultaService {

    public static final int TAMANO_PAGINA = 20;

    @Autowired
    private ProductoBusquedaRepository repositorio;

    private String escaparLike(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private Sort ordenPorNombre() {
        return Sort.by(Sort.Order.asc("nombre").ignoreCase(), Sort.Order.asc("id"));
    }

    private Specification<Producto> construirFiltro(String texto, String categoria, String estado) {

        String buscado = texto == null ? "" : texto.trim();
        String categoriaElegida = categoria == null ? "" : categoria.trim();
        String estadoElegido = estado == null ? "" : estado.trim();

        return (root, query, cb) -> {

            List<Predicate> condiciones = new ArrayList<>();

            if (!buscado.isEmpty()) {
                String patron = "%" + escaparLike(buscado.toLowerCase()) + "%";

                condiciones.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.<String>get("codigo"), "")), patron, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("nombre"), "")), patron, '\\'),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("categoria"), "")), patron, '\\')));
            }

            if (!categoriaElegida.isEmpty()) {
                condiciones.add(cb.equal(
                        cb.lower(root.<String>get("categoria")), categoriaElegida.toLowerCase()));
            }

            if ("agotado".equals(estadoElegido)) {
                condiciones.add(cb.equal(root.<Integer>get("cantidad"), 0));

            } else if ("bajo".equals(estadoElegido)) {
                condiciones.add(cb.between(root.<Integer>get("cantidad"), 1, 5));

            } else if ("disponible".equals(estadoElegido)) {
                condiciones.add(cb.greaterThan(root.<Integer>get("cantidad"), 5));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    public List<String> categorias() {
        return repositorio.categoriasDistintas();
    }

    public List<Producto> buscarTodos(String texto, String categoria, String estado) {
        return repositorio.findAll(construirFiltro(texto, categoria, estado), ordenPorNombre());
    }

    public PaginaVista<Producto> buscar(
            String texto,
            String categoria,
            String estado,
            int pagina) {

        Specification<Producto> filtro = construirFiltro(texto, categoria, estado);
        int actual = Math.max(1, pagina);

        Page<Producto> resultado = repositorio.findAll(
                filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, ordenPorNombre()));

        if (resultado.getTotalPages() > 0 && actual > resultado.getTotalPages()) {
            actual = resultado.getTotalPages();
            resultado = repositorio.findAll(
                    filtro, PageRequest.of(actual - 1, TAMANO_PAGINA, ordenPorNombre()));
        }

        return new PaginaVista<>(
                resultado.getContent(),
                actual,
                TAMANO_PAGINA,
                resultado.getTotalPages(),
                resultado.getTotalElements());
    }
}