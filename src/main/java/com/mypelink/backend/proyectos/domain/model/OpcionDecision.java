package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "opcion_decision")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class OpcionDecision {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_id", nullable = false)
    private NodoDecision nodo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nodo_destino_id")
    private NodoDecision nodoDestino;       // opción que lleva a otra pregunta

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_proyecto_id")
    private TipoProyecto tipoProyecto;      // opción hoja: resultado final

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}