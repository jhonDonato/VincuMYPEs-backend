package com.mypelink.backend.usuarios.application.service;

import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.application.dto.ActualizarLimiteProyectosRequest;
import com.mypelink.backend.usuarios.application.dto.EstudianteAdminResponse;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminEstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final PostulacionRepository postulacionRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<EstudianteAdminResponse> listarTodos(String emailAdmin) {
        validarRolAdmin(emailAdmin);
        return estudianteRepository.findAll().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional
    public EstudianteAdminResponse actualizarLimiteProyectos(
            Long estudianteId,
            ActualizarLimiteProyectosRequest request,
            String emailAdmin) {
        validarRolAdmin(emailAdmin);

        Estudiante estudiante = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante", estudianteId));

        long activos = contarProyectosActivos(estudiante.getId());
        if (request.nuevoLimite() < activos) {
            throw new BusinessException(
                    "No puedes bajar el límite a " + request.nuevoLimite() +
                            " porque el estudiante tiene " + activos +
                            " proyectos activos. Espera a que terminen alguno.",
                    HttpStatus.CONFLICT
            );
        }

        estudiante.setLimiteProyectos(request.nuevoLimite());
        estudianteRepository.save(estudiante);
        return toAdminResponse(estudiante);
    }

    // ─────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────

    private void validarRolAdmin(String email) {
        var usuario = usuarioRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        if (!usuario.getRol().getNombre().equals("ROLE_ADMIN")) {
            throw new BusinessException("Acceso denegado: se requiere rol Administrador",
                    HttpStatus.FORBIDDEN);
        }
    }

    private long contarProyectosActivos(Long estudianteId) {
        List<WorkflowEstado> estadosActivos = List.of(
                WorkflowEstado.PENDIENTE,
                WorkflowEstado.EN_DESARROLLO,
                WorkflowEstado.EN_REVISION
        );
        return postulacionRepository.countByEstudianteIdAndEstadoAndProyectoEstadoIn(
                estudianteId,
                EstadoPostulacion.CONFIRMADO,
                estadosActivos
        );
    }

    private EstudianteAdminResponse toAdminResponse(Estudiante e) {
        return new EstudianteAdminResponse(
                e.getId(),
                e.getUsuario().getNombre(),
                e.getUsuario().getEmail(),
                e.getCodigoEstudiante(),
                e.getUniversidad(),
                e.getCarrera(),
                e.getLimiteProyectos(),
                contarProyectosActivos(e.getId()),
                e.getActivo()
        );
    }
    @Transactional
    public EstudianteAdminResponse actualizarLimitePorUsuarioId(
            Long usuarioId,
            ActualizarLimiteProyectosRequest request,
            String emailAdmin) {
        validarRolAdmin(emailAdmin);

        Estudiante estudiante = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró un estudiante con usuarioId: " + usuarioId));

        long activos = contarProyectosActivos(estudiante.getId());
        if (request.nuevoLimite() < activos) {
            throw new BusinessException(
                    "No puedes bajar el límite a " + request.nuevoLimite() +
                            " porque el estudiante tiene " + activos +
                            " proyectos activos. Espera a que terminen alguno.",
                    HttpStatus.CONFLICT
            );
        }

        estudiante.setLimiteProyectos(request.nuevoLimite());
        estudianteRepository.save(estudiante);
        return toAdminResponse(estudiante);
    }
}