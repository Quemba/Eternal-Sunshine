package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.DetalleVenta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Integer> {

    List<DetalleVenta> findByProducto_Id(Integer productoId);

    boolean existsByProducto_Id(Integer productoId);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleVenta d "
            + "WHERE d.venta.fecha >= :desde AND d.venta.fecha < :hasta "
            + "AND (d.venta.anulada IS NULL OR d.venta.anulada = false)")
    Long unidadesEntre(@Param("desde") LocalDateTime desde,
                       @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(d.cantidad), 0) FROM DetalleVenta d "
            + "WHERE d.venta.usuario.id = :usuarioId "
            + "AND d.venta.fecha >= :desde AND d.venta.fecha < :hasta "
            + "AND (d.venta.anulada IS NULL OR d.venta.anulada = false)")
    Long unidadesPorUsuarioEntre(@Param("usuarioId") Integer usuarioId,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta);

    @Query("SELECT d.producto.nombre, SUM(d.cantidad) FROM DetalleVenta d "
            + "WHERE d.venta.fecha >= :desde "
            + "AND (d.venta.anulada IS NULL OR d.venta.anulada = false) "
            + "GROUP BY d.producto.id, d.producto.nombre "
            + "ORDER BY SUM(d.cantidad) DESC")
    List<Object[]> masVendidos(@Param("desde") LocalDateTime desde, Pageable pageable);

    @Query("SELECT d.producto.id, SUM(d.cantidad) FROM DetalleVenta d "
            + "WHERE d.venta.fecha >= :desde "
            + "AND (d.venta.anulada IS NULL OR d.venta.anulada = false) "
            + "GROUP BY d.producto.id")
    List<Object[]> unidadesPorProductoDesde(@Param("desde") LocalDateTime desde);
}