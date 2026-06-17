package com.mypelink.backend.certificaciones.domain.repository;

import com.mypelink.backend.certificaciones.domain.model.Certificado;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {

    // Método original (sin cambios) – puede tener N+1 pero no rompe
    List<Certificado> findByEstudianteUsuarioEmail(String email);

    // Método original (sin cambios)
    List<Certificado> findByProyectoMypeUsuarioEmail(String email);

    // ✅ NUEVO: con JOIN FETCH para evitar N+1
    @Query("SELECT c FROM Certificado c " +
            "JOIN FETCH c.estudiante e " +
            "JOIN FETCH e.usuario " +
            "JOIN FETCH c.proyecto p " +
            "JOIN FETCH p.mype m " +
            "JOIN FETCH m.usuario " +
            "WHERE e.usuario.email = :email")
    List<Certificado> findByEstudianteUsuarioEmailEager(@Param("email") String email);

    // ✅ NUEVO: con JOIN FETCH para MYPE
    @Query("SELECT c FROM Certificado c " +
            "JOIN FETCH c.proyecto p " +
            "JOIN FETCH p.mype m " +
            "JOIN FETCH m.usuario " +
            "JOIN FETCH c.estudiante e " +
            "JOIN FETCH e.usuario " +
            "WHERE m.usuario.email = :email")
    List<Certificado> findByProyectoMypeUsuarioEmailEager(@Param("email") String email);

    Optional<Certificado> findByCodigo(String codigo);

    boolean existsByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);

    // ═══════════════════════════════════════════
    // ✅ AGREGAR ESTE MÉTODO NUEVO
    // ═══════════════════════════════════════════
    @Query("SELECT c FROM Certificado c " +
            "LEFT JOIN FETCH c.emitidoPor ep " +
            "LEFT JOIN FETCH c.estudiante e " +
            "LEFT JOIN FETCH e.usuario eu " +
            "LEFT JOIN FETCH c.proyecto p " +
            "LEFT JOIN FETCH p.mype m " +
            "LEFT JOIN FETCH m.usuario mu " +
            "WHERE c.id = :id")
    Optional<Certificado> findByIdWithRelations(@Param("id") Long id);
}