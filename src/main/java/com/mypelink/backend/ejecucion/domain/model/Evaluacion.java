package com.mypelink.backend.ejecucion.domain.model;

import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Mype;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "evaluaciones",
    uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "estudiante_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluado_por", nullable = false)
    private Mype evaluadoPor;

    @Column(nullable = false)
    private Integer puntualidad;

    @Column(name = "calidad_trabajo", nullable = false)
    private Integer calidadTrabajo;

    @Column(nullable = false)
    private Integer comunicacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_evaluacion", nullable = false, updatable = false)
    private LocalDateTime fechaEvaluacion;

    @PrePersist
    protected void onCreate() {
        this.fechaEvaluacion = LocalDateTime.now();
    }
}
