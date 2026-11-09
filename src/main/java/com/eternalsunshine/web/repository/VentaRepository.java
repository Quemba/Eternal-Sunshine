package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Venta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Integer> {

    List<Venta> findAllByOrderByFechaDesc();

    List<Venta> findByUsuario_IdOrderByFechaDesc(Integer usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Venta v WHERE v.id = :id")
    Optional<Venta> buscarParaActualizar(@Param("id") Integer id);

    @Query("SELECT v FROM Venta v "
            + "WHERE v.fecha >= :desde AND (v.anulada IS NULL OR v.anulada = false)")
    List<Venta> ventasVigentesDesde(@Param("desde") LocalDateTime desde);

    List<Venta> findByConsecutivoIsNullOrderByFechaAscIdAsc();

    @Query("SELECT COALESCE(MAX(v.consecutivo), 0) FROM Venta v WHERE v.periodo = :periodo")
    Integer maximoConsecutivo(@Param("periodo") String periodo);

    @Query("SELECT v.periodo, MAX(v.consecutivo) FROM Venta v WHERE v.periodo IS NOT NULL GROUP BY v.periodo")
    List<Object[]> maximosPorPeriodo();

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v "
            + "WHERE v.fecha >= :desde AND v.fecha < :hasta "
            + "AND (v.anulada IS NULL OR v.anulada = false)")
    Long totalEntre(@Param("desde") LocalDateTime desde,
                    @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v "
            + "WHERE v.fecha >= :desde AND v.fecha < :hasta "
            + "AND (v.anulada IS NULL OR v.anulada = false)")
    Long contarEntre(@Param("desde") LocalDateTime desde,
                     @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v "
            + "WHERE v.usuario.id = :usuarioId AND v.fecha >= :desde AND v.fecha < :hasta "
            + "AND (v.anulada IS NULL OR v.anulada = false)")
    Long totalPorUsuarioEntre(@Param("usuarioId") Integer usuarioId,
                              @Param("desde") LocalDateTime desde,
                              @Param("hasta") LocalDateTime hasta);

    @Query("SELECT COUNT(v) FROM Venta v "
            + "WHERE v.usuario.id = :usuarioId AND v.fecha >= :desde AND v.fecha < :hasta "
            + "AND (v.anulada IS NULL OR v.anulada = false)")
    Long contarPorUsuarioEntre(@Param("usuarioId") Integer usuarioId,
                               @Param("desde") LocalDateTime desde,
                               @Param("hasta") LocalDateTime hasta);

    @Query("SELECT MIN(v.fecha) FROM Venta v "
            + "WHERE v.fecha >= :desde AND (v.anulada IS NULL OR v.anulada = false)")
    LocalDateTime primeraVentaDesde(@Param("desde") LocalDateTime desde);
}