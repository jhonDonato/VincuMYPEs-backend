package com.mypelink.backend.comunicacion.domain.model;

import com.mypelink.backend.usuarios.domain.model.Usuario;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "miembros_chat_grupo",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_grupo_id", "usuario_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiembroChatGrupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // A qué chat pertenece
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_grupo_id", nullable = false)
    private ChatGrupo chatGrupo;

    // Qué usuario es miembro
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    // Cuándo se unió
    @Column(name = "fecha_union", nullable = false, updatable = false)
    private LocalDateTime fechaUnion;

    @PrePersist
    protected void onCreate() {
        this.fechaUnion = LocalDateTime.now();
    }
}