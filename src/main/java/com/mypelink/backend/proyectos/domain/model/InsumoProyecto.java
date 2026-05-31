// src/main/java/com/mypelink/backend/proyectos/domain/model/InsumoProyecto.java
package com.mypelink.backend.proyectos.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "insumos_proyecto")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class InsumoProyecto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_tipo_id")
    private InsumoTipo insumoTipo;

    @Column(name = "valor_texto", columnDefinition = "TEXT")
    private String valorTexto;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @Column(name = "fecha_subida", nullable = false, updatable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    protected void onCreate() {
        this.fechaSubida = LocalDateTime.now();
    }
}