package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Mype;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MypeRepository extends JpaRepository<Mype, Long> {
    Optional<Mype> findByUsuarioId(Long usuarioId);
    boolean existsByRuc(String ruc);
}