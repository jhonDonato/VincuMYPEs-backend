package com.mypelink.backend.ejecucion.domain.repository;

import com.mypelink.backend.ejecucion.domain.model.Evaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Long> {

    @Query("SELECT e FROM Evaluacion e JOIN FETCH e.proyecto p JOIN FETCH e.estudiante est JOIN FETCH est.usuario u JOIN FETCH e.evaluadoPor m ORDER BY e.fechaEvaluacion DESC")
    List<Evaluacion> findAllWithDetails();
}
