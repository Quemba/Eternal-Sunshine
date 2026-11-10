package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRecuperacionRepository
        extends JpaRepository<TokenRecuperacion, Integer> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update TokenRecuperacion t set t.usado = true where t.usuarioId = :usuarioId and t.usado = false")
    int invalidarPendientesDe(@Param("usuarioId") Integer usuarioId);

    @Modifying
    @Query("delete from TokenRecuperacion t where t.expira < :limite")
    int borrarVencidosAntesDe(@Param("limite") LocalDateTime limite);
}
