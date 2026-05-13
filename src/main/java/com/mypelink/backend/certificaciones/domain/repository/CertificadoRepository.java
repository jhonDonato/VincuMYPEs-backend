package com.mypelink.backend.certificaciones.domain.repository;

import com.mypelink.backend.certificaciones.domain.model.Certificado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificadoRepository extends JpaRepository<Certificado, Long> {
    
    List<Certificado> findByEstudianteUsuarioEmail(String email);
    
    List<Certificado> findByProyectoMypeUsuarioEmail(String email);
    
    Optional<Certificado> findByCodigo(String codigo);
    
    boolean existsByProyectoIdAndEstudianteId(Long proyectoId, Long estudianteId);
}
