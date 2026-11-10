package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Tienda;
import com.eternalsunshine.web.repository.TiendaRepository;
import com.eternalsunshine.web.service.CategoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CategoriasInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CategoriasInicialRunner.class);

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    public void run(ApplicationArguments args) {

        for (Tienda tienda : tiendaRepository.findAll()) {
            TiendaContexto.ejecutar(tienda.getId(), () -> prepararTienda(tienda.getId()));
        }
    }

    private void prepararTienda(Integer tiendaId) {

        try {
            int cambios = categoriaService.unificarExistentes();

            if (cambios > 0) {
                log.info("Tienda {}: categorías unificadas en {} productos.", tiendaId, cambios);
            }
        } catch (Exception e) {
            log.error("No se pudieron unificar las categorías existentes: {}", e.toString(), e);
        }

        try {
            int sembrados = categoriaService.sembrarAprendizajeSiVacio();

            if (sembrados > 0) {
                log.info("Tienda {}: aprendizaje de categorías inicializado con {} productos.", tiendaId, sembrados);
            }
        } catch (Exception e) {
            log.error("No se pudo inicializar el aprendizaje de categorías: {}", e.toString(), e);
        }
    }
}
