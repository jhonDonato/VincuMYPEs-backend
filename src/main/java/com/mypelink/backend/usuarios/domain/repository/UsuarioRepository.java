package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM Usuario u JOIN FETCH u.rol WHERE u.email = :email")
    Optional<Usuario> findByEmailWithRole(@Param("email") String email);
}