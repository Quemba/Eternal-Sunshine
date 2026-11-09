package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.ConteoInventario;
import com.eternalsunshine.web.entity.EstadoConteo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConteoRepository extends JpaRepository<ConteoInventario, Integer> {

    List<ConteoInventario> findTop100ByOrderByFechaInicioDescIdDesc();

    List<ConteoInventario> findTop100ByEstadoOrderByFechaInicioDescIdDesc(EstadoConteo estado);

    List<ConteoInventario> findTop100ByUsuario_IdOrderByFechaInicioDescIdDesc(Integer usuarioId);

    List<ConteoInventario> findTop100ByUsuario_IdAndEstadoOrderByFechaInicioDescIdDesc(
            Integer usuarioId, EstadoConteo estado);

    long countByEstado(EstadoConteo estado);

    List<ConteoInventario> findByConsecutivoIsNullAndEstadoInOrderByFechaInicioAscIdAsc(
            Collection<EstadoConteo> estados);

    @Query("SELECT COALESCE(MAX(c.consecutivo), 0) FROM ConteoInventario c WHERE c.periodo = :periodo")
    Integer maximoConsecutivo(@Param("periodo") String periodo);

    @Query("SELECT c.periodo, MAX(c.consecutivo) FROM ConteoInventario c "
            + "WHERE c.periodo IS NOT NULL GROUP BY c.periodo")
    List<Object[]> maximosPorPeriodo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ConteoInventario c WHERE c.id = :id")
    Optional<ConteoInventario> buscarParaActualizar(@Param("id") Integer id);
}