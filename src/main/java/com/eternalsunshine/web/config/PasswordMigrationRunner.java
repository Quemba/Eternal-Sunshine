package com.eternalsunshine.web.config;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    private static final String FORMATO_BCRYPT = "^\\$2[aby]\\$\\d{2}\\$.{53}$";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordMigrationRunner(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        for (Usuario usuario : usuarioRepository.findAll()) {

            String password = usuario.getPassword();

            if (password == null || password.isEmpty()) {
                continue;
            }

            if (!password.matches(FORMATO_BCRYPT)) {
                usuario.setPassword(passwordEncoder.encode(password));
                usuarioRepository.save(usuario);
            }
        }
    }
}