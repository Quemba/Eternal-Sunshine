package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TiendaRepository extends JpaRepository<Tienda, Integer> {
}
