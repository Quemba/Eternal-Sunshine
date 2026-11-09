package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.ContadorDocumento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContadorDocumentoRepository extends JpaRepository<ContadorDocumento, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ContadorDocumento c WHERE c.tipo = :tipo AND c.periodo = :periodo")
    Optional<ContadorDocumento> buscarParaActualizar(@Param("tipo") String tipo,
                                                     @Param("periodo") String periodo);

    Optional<ContadorDocumento> findByTipoAndPeriodo(String tipo, String periodo);
}