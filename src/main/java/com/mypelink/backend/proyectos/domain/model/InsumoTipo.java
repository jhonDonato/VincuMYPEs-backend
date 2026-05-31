// src/main/java/com/mypelink/backend/proyectos/domain/model/InsumoTipo.java
package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "insumos_tipo")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InsumoTipo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_proyecto_id", nullable = false)
    private TipoProyecto tipoProyecto;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(length = 50)
    private String formato; // PDF, IMAGEN, EXCEL, TEXTO, LINK

    @Column(nullable = false)
    @Builder.Default
    private Boolean obligatorio = false;

    @Column(nullable = false)
    @Builder.Default
    private Integer orden = 0;
}