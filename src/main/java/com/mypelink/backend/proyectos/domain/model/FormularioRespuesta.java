package com.mypelink.backend.proyectos.domain.model;

import com.mypelink.backend.usuarios.domain.model.Mype;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "formulario_respuestas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FormularioRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mype_id", nullable = false)
    private Mype mype;

    @Column(name = "pregunta_1_tipo_problema", nullable = false, length = 100)
    private String pregunta1TipoProblema;

    @Column(name = "pregunta_2_alcance", nullable = false, length = 100)
    private String pregunta2Alcance;

    @Column(name = "pregunta_3_urgencia", length = 50)
    private String pregunta3Urgencia;

    @Column(name = "descripcion_libre", columnDefinition = "TEXT")
    private String descripcionLibre;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        this.fechaEnvio = LocalDateTime.now();
    }
}

