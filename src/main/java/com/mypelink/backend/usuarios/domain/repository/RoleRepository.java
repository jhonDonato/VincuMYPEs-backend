package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByNombre(String nombre);
}