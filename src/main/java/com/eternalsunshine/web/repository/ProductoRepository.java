package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository
        extends JpaRepository<Producto, Integer> {

    long countByCantidadBetween(Integer min, Integer max);

    long countByCantidad(
            Integer cantidad);

    Producto findByCodigoIgnoreCase(String codigo);

    List<Producto> findTop5ByCantidadLessThanEqualOrderByCantidadAsc(Integer cantidad);

    List<Producto> findByCantidadLessThanEqualOrderByCantidadAscNombreAsc(Integer cantidad);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Producto p WHERE p.id = :id")
    Optional<Producto> buscarParaActualizar(@Param("id") Integer id);

    @Query("""
           SELECT COALESCE(
           SUM(p.precio * p.cantidad),0)
           FROM Producto p
           """)
    Long calcularValorInventario();

    @Query("""
       SELECT p
       FROM Producto p
       WHERE LOWER(p.nombre)
       LIKE LOWER(CONCAT('%', :texto, '%')) ESCAPE '\\'
       OR LOWER(p.codigo)
       LIKE LOWER(CONCAT('%', :texto, '%')) ESCAPE '\\'
       OR LOWER(p.categoria)
       LIKE LOWER(CONCAT('%', :texto, '%')) ESCAPE '\\'
       """)
    List<Producto> buscar(
            @Param("texto")
            String texto);
}