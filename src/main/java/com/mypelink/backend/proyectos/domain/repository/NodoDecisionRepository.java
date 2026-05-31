package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.NodoDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NodoDecisionRepository extends JpaRepository<NodoDecision, Long> {
    Optional<NodoDecision> findByCodigo(String codigo);
    List<NodoDecision> findByActivoTrueOrderByOrdenAsc();
    List<NodoDecision> findByEsRaizTrueAndActivoTrue();
}