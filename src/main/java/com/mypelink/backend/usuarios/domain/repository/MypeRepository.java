package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Mype;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MypeRepository extends JpaRepository<Mype, Long> {
    Optional<Mype> findByUsuarioId(Long usuarioId);
    boolean existsByRuc(String ruc);
    List<Mype> findByUsuarioIdIn(Set<Long> usuarioIds);

}