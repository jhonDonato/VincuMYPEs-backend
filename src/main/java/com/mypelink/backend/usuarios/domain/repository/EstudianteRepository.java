package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByUsuarioId(Long usuarioId);
    boolean existsByCodigoEstudiante(String codigoEstudiante);
    @Query("SELECT e FROM Estudiante e JOIN FETCH e.usuario u JOIN FETCH u.rol WHERE u.email = :email")
    Optional<Estudiante> findByUsuarioEmailWithRole(@Param("email") String email);

}