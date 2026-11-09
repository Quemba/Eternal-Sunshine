package com.eternalsunshine.web.repository;

import com.eternalsunshine.web.entity.AprendizajeCategoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AprendizajeCategoriaRepository extends JpaRepository<AprendizajeCategoria, Integer> {

    List<AprendizajeCategoria> findByPalabraIn(Collection<String> palabras);

    Optional<AprendizajeCategoria> findByPalabraAndCategoria(String palabra, String categoria);
}