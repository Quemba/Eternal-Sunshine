package com.eternalsunshine.web.config;

import com.eternalsunshine.web.repository.UsuarioRepository;
import com.eternalsunshine.web.service.TiendaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AdminInicialRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialRunner.class);

    @Value("${app.admin.username:}")
    private String username;

    @Value("${app.admin.password:}")
    private String password;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TiendaService tiendaService;

    @Value("${app.admin.tienda:Tienda principal}")
    private String nombreTienda;

    @Override
    public void run(ApplicationArguments args) {

        try {
            if (usuarioRepository.count() > 0) {
                return;
            }

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                log.warn("No hay usuarios y no se definieron ADMIN_USERNAME y ADMIN_PASSWORD: nadie podrá iniciar sesión.");
                return;
            }

            tiendaService.crearConAdmin(nombreTienda, username, password);

            log.info("Administrador inicial «{}» creado.", username.trim());

        } catch (Exception e) {
            log.error("No se pudo crear el administrador inicial: {}", e.getMessage());
        }
    }
}