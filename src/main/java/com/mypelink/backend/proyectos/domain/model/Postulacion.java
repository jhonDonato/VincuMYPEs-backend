package com.mypelink.backend.proyectos.domain.model;

import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "postulaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(name = "mensaje_postulacion", length = 200)
    private String mensajePostulacion;

    @Column(name = "archivo_adjunto", length = 255)
    private String archivoAdjunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EstadoPostulacion estado = EstadoPostulacion.PENDIENTE;

    @Column(name = "fecha_postulacion", nullable = false, updatable = false)
    private LocalDateTime fechaPostulacion;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluado_por_admin_id")
    private Usuario evaluadoPorAdmin;

    @Column(name = "observaciones_respuesta", columnDefinition = "TEXT")
    private String observacionesRespuesta;

    @PrePersist
    protected void onCreate() {
        this.fechaPostulacion = LocalDateTime.now();
    }
}

