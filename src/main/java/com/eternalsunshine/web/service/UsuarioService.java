package com.eternalsunshine.web.service;

import com.eternalsunshine.web.entity.Usuario;
import com.eternalsunshine.web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public Usuario login(
            String username,
            String password) {

        return usuarioRepository
                .findByUsernameAndPassword(
                        username.trim(),
                        password.trim());
    }

    public Usuario buscarPorUsername(
        String username) {

    return usuarioRepository
            .findByUsername(username);
}

public List<Usuario> obtenerUsuarios() {

    return usuarioRepository.findAll();
}
}