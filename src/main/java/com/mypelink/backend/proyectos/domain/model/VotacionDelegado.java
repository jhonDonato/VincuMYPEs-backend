package com.mypelink.backend.proyectos.domain.model;

import com.mypelink.backend.shared.domain.enums.FaseVotacion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votaciones_delegado")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotacionDelegado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Un proyecto tiene UNA votación activa a la vez
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    // Fecha límite para votar (48h desde que se abre)
    @Column(name = "fecha_limite", nullable = false)
    private LocalDateTime fechaLimite;

    // Estado de la votación
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private FaseVotacion estado = FaseVotacion.EN_VOTACION;

    // Quién ganó (FK a Postulacion)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postulacion_ganadora_id")
    private Postulacion postulacionGanadora;

    // Cuándo se creó
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estado == null) this.estado = FaseVotacion.EN_VOTACION;
    }
}