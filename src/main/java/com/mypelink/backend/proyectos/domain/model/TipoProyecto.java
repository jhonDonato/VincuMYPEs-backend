package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipo_proyecto")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoProyecto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion_mype", columnDefinition = "TEXT")
    private String descripcionMype;

    @Column(name = "descripcion_estudiante", columnDefinition = "TEXT")
    private String descripcionEstudiante;

    @Column(nullable = false, length = 50)
    private String rama;

    @Column(name = "ciclo_minimo", nullable = false)
    private Integer cicloMinimo;

    @Column(name = "incluye_presupuesto", nullable = false)
    @Builder.Default
    private Boolean incluyePresupuesto = false;

    @Column(name = "incluye_cronograma", nullable = false)
    @Builder.Default
    private Boolean incluyeCronograma = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}