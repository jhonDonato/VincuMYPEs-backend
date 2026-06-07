// com.mypelink.backend.calificaciones.domain.model.Calificacion
package com.mypelink.backend.calificaciones.domain.model;

import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "calificaciones",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_calificacion_unica",
                columnNames = {"proyecto_id", "calificador_id", "calificado_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calificador_id", nullable = false)
    private Usuario calificador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calificado_id", nullable = false)
    private Usuario calificado;

    @Column(nullable = false)
    private Integer puntuacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}