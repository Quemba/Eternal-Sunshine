package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.DetalleConteo;
import com.eternalsunshine.web.entity.EstadoLineaConteo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface DetalleConteoRepository extends JpaRepository<DetalleConteo, Integer> {

    boolean existsByProducto_Id(Integer productoId);

    @Query("SELECT COUNT(d) FROM DetalleConteo d "
            + "WHERE d.producto.id = :productoId "
            + "AND d.estado = :estado "
            + "AND d.fechaAplicacion > :fecha "
            + "AND d.conteo.id <> :conteoId")
    long contarAplicadasDespuesDe(@Param("productoId") Integer productoId,
                                  @Param("estado") EstadoLineaConteo estado,
                                  @Param("fecha") LocalDateTime fecha,
                                  @Param("conteoId") Integer conteoId);
}