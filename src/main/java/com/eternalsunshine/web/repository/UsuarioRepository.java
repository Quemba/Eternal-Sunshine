package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    Usuario findByUsernameAndPassword(
            String username,
            String password);

    Usuario findByUsername(
            String username);
}