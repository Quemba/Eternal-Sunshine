package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Historial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialBusquedaRepository
        extends JpaRepository<Historial, Integer>, JpaSpecificationExecutor<Historial> {
}