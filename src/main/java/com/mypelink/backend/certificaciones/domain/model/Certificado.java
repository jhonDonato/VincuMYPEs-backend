package com.mypelink.backend.certificaciones.domain.model;

import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "certificados",
    uniqueConstraints = @UniqueConstraint(columnNames = {"proyecto_id", "estudiante_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @Column(nullable = false, unique = true, length = 100, updatable = false)
    private String codigo;

    @Column(name = "titulo_certificado", nullable = false, length = 300)
    private String tituloCertificado;

    @Column(name = "descripcion_certificado", columnDefinition = "TEXT")
    private String descripcionCertificado;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDate fechaEmision;

    @Column(name = "url_certificado", length = 255)
    private String urlCertificado;

    @Column(name = "qr_code", length = 255)
    private String qrCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emitido_por")
    private Usuario emitidoPor;

    @Column(nullable = false)
    private Boolean activo;

    @Column(name = "enviado_email", nullable = false)
    private Boolean enviadoEmail = false;

    @PrePersist
    protected void onCreate() {
        this.fechaEmision = LocalDate.now();
        if (this.activo == null) this.activo = true;
    }
}

