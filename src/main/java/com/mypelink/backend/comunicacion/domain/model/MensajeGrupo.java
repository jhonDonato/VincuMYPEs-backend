package com.mypelink.backend.comunicacion.domain.model;

import com.mypelink.backend.usuarios.domain.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_grupo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MensajeGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué chat grupal pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_grupo_id", nullable = false)
    private ChatGrupo chatGrupo;

    // Quién envió el mensaje
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remitente_id", nullable = false)
    private Usuario remitente;

    // Contenido del mensaje
    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensaje;

    // Archivo adjunto (opcional)
    @Column(name = "archivo_adjunto", length = 255)
    private String archivoAdjunto;

    // Cuándo se envió
    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    @PrePersist
    protected void onCreate() {
        this.fechaEnvio = LocalDateTime.now();
    }
}