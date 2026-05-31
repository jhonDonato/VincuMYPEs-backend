package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nodo_decision")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class NodoDecision {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;           // "inicio", "mostrar_internet", etc.

    @Column(nullable = false, columnDefinition = "TEXT")
    private String pregunta;

    @Column(name = "tiene_input_libre", nullable = false)
    @Builder.Default
    private Boolean tieneInputLibre = false;

    @Column(name = "input_placeholder", length = 200)
    private String inputPlaceholder;

    @Column(name = "es_raiz", nullable = false)
    @Builder.Default
    private Boolean esRaiz = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activo = true;
}