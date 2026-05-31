package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "entregables_tipo")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EntregableTipo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_proyecto_id", nullable = false)
    private TipoProyecto tipoProyecto;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}