package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByUsuarioId(Long usuarioId);
    boolean existsByCodigoEstudiante(String codigoEstudiante);
}