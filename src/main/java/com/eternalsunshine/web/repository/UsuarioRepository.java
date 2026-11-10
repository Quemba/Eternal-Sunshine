package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Usuario;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository
        extends JpaRepository<Usuario, Integer> {

    Usuario findByUsernameIgnoreCase(
            String username);

    List<Usuario> findByEmailIgnoreCase(
            String email);

    List<Usuario> findByTiendaId(
            Integer tiendaId);

    List<Usuario> findByTiendaId(
            Integer tiendaId,
            Sort sort);
}
