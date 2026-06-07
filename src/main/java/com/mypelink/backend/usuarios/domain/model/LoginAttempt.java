package com.mypelink.backend.usuarios.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(name = "attempt_time", nullable = false)
    private LocalDateTime attemptTime;

    @Column(name = "blocked_until")
    private LocalDateTime blockedUntil;
}