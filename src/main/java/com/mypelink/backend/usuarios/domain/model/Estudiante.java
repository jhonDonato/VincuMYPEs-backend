package com.mypelink.backend.usuarios.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "estudiantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "codigo_estudiante", unique = true, length = 50)
    private String codigoEstudiante;

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String universidad = "Universidad Privada del Norte";

    @Column(nullable = false, length = 100)
    @Builder.Default
    private String carrera = "Ingeniería de Sistemas Computacionales";

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(name = "portafolio_url", length = 255)
    private String portafolioUrl;

    @Column(name = "linkedin_url", length = 255)
    private String linkedinUrl;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Column(name = "limite_proyectos", nullable = false, columnDefinition = "integer default 2")
    @Builder.Default
    private Integer limiteProyectos = 2;

    @Column(nullable = false)
    private Boolean activo;

    public Integer getLimiteProyectos() {
        return (limiteProyectos != null && limiteProyectos > 0) ? limiteProyectos : 2;
    }

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    // ========== NUEVOS CAMPOS DE UBICACIÓN ==========

    @Column(length = 100)
    private String ciudad;

    @Column(length = 100)
    private String pais;

    @Column(length = 100)
    private String sector;  // Distrito o sector

    @Column(length = 100)
    private String barrio;

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;


    @PrePersist
    protected void onCreate() {
        this.fechaRegistro = LocalDateTime.now();
        if (this.activo == null) this.activo = true;
    }
}
