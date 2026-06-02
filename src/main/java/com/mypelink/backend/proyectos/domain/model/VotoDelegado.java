package com.mypelink.backend.proyectos.domain.model;

import com.mypelink.backend.usuarios.domain.model.Estudiante;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "votos_delegado",
        uniqueConstraints = @UniqueConstraint(columnNames = {"votacion_id", "votante_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VotoDelegado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué votación pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votacion_id", nullable = false)
    private VotacionDelegado votacion;

    // Quién vota (el estudiante que emite el voto)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "votante_id", nullable = false)
    private Estudiante votante;

    // Por quién vota (otro estudiante del equipo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidato_id", nullable = false)
    private Estudiante candidato;

    // No se puede votar a sí mismo
    @Column(name = "fecha_voto", nullable = false, updatable = false)
    private LocalDateTime fechaVoto;

    @PrePersist
    protected void onCreate() {
        this.fechaVoto = LocalDateTime.now();
    }
}