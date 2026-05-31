package com.mypelink.backend.proyectos.domain.repository;

import com.mypelink.backend.proyectos.domain.model.OpcionDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OpcionDecisionRepository extends JpaRepository<OpcionDecision, Long> {
    List<OpcionDecision> findByNodoIdAndActivoTrueOrderByOrden(Long nodoId);

    @Query("SELECT o FROM OpcionDecision o LEFT JOIN FETCH o.tipoProyecto WHERE o.nodo.id = :nodoId AND o.activo = true ORDER BY o.orden")
    List<OpcionDecision> findByNodoIdWithTipoProyecto(@Param("nodoId") Long nodoId);
    Optional<OpcionDecision> findByNodoIdAndOrden(Long nodoId, Integer orden);
    List<OpcionDecision> findByNodoDestinoId(Long nodoDestinoId);
    @Query("SELECT o FROM OpcionDecision o JOIN FETCH o.nodo WHERE o.nodoDestino.id = :nodoDestinoId")
    List<OpcionDecision> findByNodoDestinoIdWithNodo(@Param("nodoDestinoId") Long nodoDestinoId);
}