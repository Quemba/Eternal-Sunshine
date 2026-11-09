package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.EstadoSolicitud;
import com.eternalsunshine.web.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Integer> {

    List<Solicitud> findByEstadoOrderByFechaSolicitudDesc(EstadoSolicitud estado);

    List<Solicitud> findByUsuarioSolicitante_IdOrderByFechaSolicitudDesc(Integer usuarioId);

    boolean existsByProducto_IdAndEstado(Integer productoId, EstadoSolicitud estado);

    boolean existsByCodigoIgnoreCaseAndEstado(String codigo, EstadoSolicitud estado);

    List<Solicitud> findByProducto_Id(Integer productoId);

    long countByEstado(EstadoSolicitud estado);

    @Query("SELECT COUNT(s) FROM Solicitud s "
            + "WHERE s.usuarioSolicitante.id = :usuarioId "
            + "AND s.estado <> :estado "
            + "AND s.vistaPorSolicitante = false")
    long contarNuevasParaUsuario(@Param("usuarioId") Integer usuarioId,
                                 @Param("estado") EstadoSolicitud estado);
}