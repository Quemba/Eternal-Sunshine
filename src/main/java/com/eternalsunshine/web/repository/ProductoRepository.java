package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository
        extends JpaRepository<Producto, Integer> {

    long countByCantidadLessThanEqual(
            Integer cantidad);

    long countByCantidad(
            Integer cantidad);

    @Query("""
           SELECT COALESCE(
           SUM(p.precio * p.cantidad),0)
           FROM Producto p
           """)
    Double calcularValorInventario();

    @Query("""
       SELECT p
       FROM Producto p
       WHERE LOWER(p.nombre)
       LIKE LOWER(CONCAT('%', :texto, '%'))
       OR LOWER(p.codigo)
       LIKE LOWER(CONCAT('%', :texto, '%'))
       OR LOWER(p.categoria)
       LIKE LOWER(CONCAT('%', :texto, '%'))
       """)
java.util.List<Producto> buscar(
        @org.springframework.data.repository.query.Param("texto")
        String texto);
}