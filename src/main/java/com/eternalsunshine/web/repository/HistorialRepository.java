package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Historial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialRepository
        extends JpaRepository<Historial, Integer> {

    boolean existsByUsuario_Id(Integer usuarioId);
}