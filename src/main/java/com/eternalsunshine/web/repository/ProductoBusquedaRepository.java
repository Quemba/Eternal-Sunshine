package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductoBusquedaRepository
        extends JpaRepository<Producto, Integer>, JpaSpecificationExecutor<Producto> {

    @Query("SELECT DISTINCT p.categoria FROM Producto p "
            + "WHERE p.categoria IS NOT NULL AND p.categoria <> '' "
            + "ORDER BY p.categoria")
    List<String> categoriasDistintas();

    List<Producto> findByCodigoEndingWith(String cola);
}