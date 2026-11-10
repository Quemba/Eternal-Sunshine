package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Tienda;
import com.eternalsunshine.web.repository.TiendaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Prepara una base de datos creada antes del multi-tienda: asigna todo lo que ya existía
 * a una tienda inicial y quita las restricciones únicas que no distinguen entre tiendas
 * (por ejemplo, que el código de un producto fuera único en todo el sistema).
 * En una base nueva no hace nada.
 */
@Component
@Order(1)
public class MigracionTiendaRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracionTiendaRunner.class);

    private static final List<String> TABLAS_DE_TIENDA = List.of(
            "productos", "historial", "solicitudes", "ventas", "detalle_ventas",
            "entradas_mercancia", "conteos_inventario", "detalles_conteo",
            "contadores_documento", "aprendizaje_categoria");

    private static final List<String> TABLAS_CON_UNICOS = List.of(
            "productos", "ventas", "conteos_inventario", "contadores_documento", "aprendizaje_categoria");

    @Value("${app.admin.tienda:Tienda principal}")
    private String nombreTiendaInicial;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    public void run(ApplicationArguments args) {

        try {
            asignarTiendaInicial();
        } catch (Exception e) {
            log.error("No se pudo asignar la tienda inicial a los datos existentes: {}", e.toString(), e);
        }

        try {
            quitarUnicosSinTienda();
        } catch (Exception e) {
            log.error("No se pudieron actualizar las restricciones únicas: {}", e.toString(), e);
        }
    }

    private long contarSinTienda(String tabla) {

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + tabla + " WHERE tienda_id IS NULL", Long.class);

        return total == null ? 0 : total;
    }

    private void asignarTiendaInicial() {

        long pendientes = contarSinTienda("usuarios");

        for (String tabla : TABLAS_DE_TIENDA) {
            pendientes += contarSinTienda(tabla);
        }

        if (pendientes == 0) {
            return;
        }

        Integer tiendaId = tiendaRepository.findAll().stream()
                .map(Tienda::getId)
                .min(Integer::compareTo)
                .orElseGet(() -> tiendaRepository.save(new Tienda(nombreTiendaInicial.trim())).getId());

        for (String tabla : TABLAS_DE_TIENDA) {
            jdbc.update("UPDATE " + tabla + " SET tienda_id = ? WHERE tienda_id IS NULL", tiendaId);
        }

        jdbc.update("UPDATE usuarios SET tienda_id = ? WHERE tienda_id IS NULL", tiendaId);

        log.info("Los datos existentes quedaron asignados a la tienda {}.", tiendaId);
    }

    private void quitarUnicosSinTienda() {

        String consulta = """
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE c.contype = 'u'
                  AND t.relname = ?
                  AND n.nspname = current_schema()
                  AND NOT EXISTS (
                      SELECT 1 FROM pg_attribute a
                      WHERE a.attrelid = t.oid
                        AND a.attnum = ANY (c.conkey)
                        AND a.attname = 'tienda_id')
                """;

        for (String tabla : TABLAS_CON_UNICOS) {

            List<String> nombres = jdbc.queryForList(consulta, String.class, tabla);

            for (String nombre : nombres) {
                jdbc.execute("ALTER TABLE " + tabla + " DROP CONSTRAINT \"" + nombre + "\"");
                log.info("Se quitó la restricción única «{}» de {} (ahora es única por tienda).", nombre, tabla);
            }
        }
    }
}
