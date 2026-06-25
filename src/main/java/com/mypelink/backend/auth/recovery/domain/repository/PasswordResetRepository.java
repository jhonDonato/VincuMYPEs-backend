package com.mypelink.backend.auth.recovery.domain.repository;

import com.mypelink.backend.auth.recovery.domain.model.PasswordReset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRepository extends JpaRepository<PasswordReset, Long> {

    @Modifying
    @Query("UPDATE PasswordReset p SET p.used = true WHERE p.email = :email AND p.used = false")
    void invalidatePreviousCodes(@Param("email") String email);

    Optional<PasswordReset> findByEmailAndOtpCodeAndUsedFalseAndExpiresAtAfter(
            @Param("email") String email,
            @Param("otpCode") String otpCode,
            @Param("now") LocalDateTime now
    );

    void deleteByExpiresAtBefore(LocalDateTime dateTime);

    List<PasswordReset> findByEmail(String email);
}