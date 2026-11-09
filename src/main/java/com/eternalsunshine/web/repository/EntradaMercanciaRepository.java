package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.EntradaMercancia;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntradaMercanciaRepository
        extends JpaRepository<EntradaMercancia, Integer>, JpaSpecificationExecutor<EntradaMercancia> {

    boolean existsByProducto_Id(Integer productoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EntradaMercancia e WHERE e.id = :id")
    Optional<EntradaMercancia> buscarParaActualizar(@Param("id") Integer id);

    @Query("SELECT e FROM EntradaMercancia e "
            + "WHERE LOWER(e.proveedor) = LOWER(:proveedor) "
            + "AND LOWER(e.numeroFactura) = LOWER(:numero) "
            + "AND (e.anulada IS NULL OR e.anulada = false) "
            + "ORDER BY e.fecha DESC, e.id DESC")
    List<EntradaMercancia> buscarFacturaRegistrada(@Param("proveedor") String proveedor,
                                                   @Param("numero") String numero);

    @Query("SELECT DISTINCT e.proveedor FROM EntradaMercancia e "
            + "WHERE e.proveedor IS NOT NULL ORDER BY e.proveedor")
    List<String> proveedoresDistintos();

    List<EntradaMercancia> findByConsecutivoIsNullOrderByFechaAscIdAsc();

    @Query("SELECT COALESCE(MAX(e.consecutivo), 0) FROM EntradaMercancia e WHERE e.periodo = :periodo")
    Integer maximoConsecutivo(@Param("periodo") String periodo);

    @Query("SELECT e.periodo, MAX(e.consecutivo) FROM EntradaMercancia e "
            + "WHERE e.periodo IS NOT NULL GROUP BY e.periodo")
    List<Object[]> maximosPorPeriodo();

    Optional<EntradaMercancia> findFirstByUsuario_IdOrderByFechaDescIdDesc(Integer usuarioId);
}