package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Tienda;
import com.eternalsunshine.web.repository.TiendaRepository;
import com.eternalsunshine.web.service.NumeracionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NumeracionInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NumeracionInicialRunner.class);

    @Autowired
    private NumeracionService numeracionService;

    @Autowired
    private TiendaRepository tiendaRepository;

    @Override
    public void run(ApplicationArguments args) {
        for (Tienda tienda : tiendaRepository.findAll()) {

            TiendaContexto.ejecutar(tienda.getId(), () -> {
                try {
                    numeracionService.rellenarPendientes();
                } catch (Exception e) {
                    log.error("No se pudo numerar los registros de la tienda {}: {}",
                            tienda.getId(), e.toString(), e);
                }
            });
        }
    }
}