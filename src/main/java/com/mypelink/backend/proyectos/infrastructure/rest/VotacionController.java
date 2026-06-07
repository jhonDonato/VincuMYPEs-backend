package com.mypelink.backend.proyectos.infrastructure.rest;

import com.mypelink.backend.proyectos.application.dto.VotacionResponse;
import com.mypelink.backend.proyectos.application.dto.VotarRequest;
import com.mypelink.backend.proyectos.application.service.VotacionService;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.model.Usuario;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/proyectos/{proyectoId}/votacion")
@RequiredArgsConstructor
public class VotacionController {

    private final VotacionService votacionService;
    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final PostulacionRepository postulacionRepository;

    // ═══════════════════════════════════════════════════════════
    // INICIAR VOTACIÓN (admin o automático)
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/iniciar")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<VotacionResponse> iniciarVotacion(
            @PathVariable Long proyectoId) {
        return ResponseEntity.ok(votacionService.iniciarVotacion(proyectoId));
    }

    // ═══════════════════════════════════════════════════════════
    // VOTAR (estudiante)
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/votar")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<VotacionResponse> votar(
            @PathVariable Long proyectoId,
            @Valid @RequestBody VotarRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                votacionService.votar(proyectoId, request, userDetails.getUsername())
        );
    }

    // ═══════════════════════════════════════════════════════════
    // OBTENER ESTADO DE VOTACIÓN (cualquier miembro del proyecto)
    // ═══════════════════════════════════════════════════════════
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VotacionResponse> obtenerVotacion(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                votacionService.obtenerVotacion(proyectoId, userDetails.getUsername())
        );
    }
    // ═══════════════════════════════════════════════════════════
    // PROPONERSE COMO CANDIDATO A DELEGADO
    // ═══════════════════════════════════════════════════════════
    @PostMapping("/proponerse")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<VotacionResponse> proponerse(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                votacionService.proponerseComoCandidato(proyectoId, userDetails.getUsername())
        );
    }

    // ═══════════════════════════════════════════════════════════
    // ✅ CORREGIDO: VERIFICAR SI EL USUARIO ACTUAL ES DELEGADO
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/es-delegado")
    @PreAuthorize("hasAnyAuthority('ROLE_ESTUDIANTE', 'ESTUDIANTE')")
    public ResponseEntity<Map<String, Boolean>> esDelegado(
            @PathVariable Long proyectoId,
            @AuthenticationPrincipal UserDetails userDetails) {

        // 1. Buscar usuario por email
        Usuario usuario = usuarioRepository.findByEmailWithRole(userDetails.getUsername())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        // 2. Buscar estudiante
        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        // 3. Buscar postulación del estudiante en este proyecto
        Postulacion postulacion = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())
                .orElse(null);

        // 4. Verificar si es delegado
        boolean esDelegado = postulacion != null &&
                postulacion.getEsDelegado() != null &&
                postulacion.getEsDelegado();

        return ResponseEntity.ok(Map.of("esDelegado", esDelegado));
    }

}