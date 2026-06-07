package com.mypelink.backend.usuarios.domain.repository;

import com.mypelink.backend.usuarios.domain.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Transactional
    void deleteAllByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByUserId(Long userId);
}