package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaBusquedaRepository
        extends JpaRepository<Venta, Integer>, JpaSpecificationExecutor<Venta> {
}