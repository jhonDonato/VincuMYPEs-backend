package com.mypelink.backend.ejecucion.application.service;

import com.mypelink.backend.ejecucion.application.dto.EntregableRequest;
import com.mypelink.backend.ejecucion.application.dto.EntregableResponse;
import com.mypelink.backend.ejecucion.application.dto.RevisarEntregableRequest;
import com.mypelink.backend.ejecucion.domain.model.Entregable;
import com.mypelink.backend.ejecucion.domain.repository.EntregableRepository;
import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.model.Estudiante;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntregableService {

    private final EntregableRepository entregableRepository;
    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final EstudianteRepository estudianteRepository;
    private final MypeRepository mypeRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Transactional
    public EntregableResponse subir(Long proyectoId, EntregableRequest request, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        boolean postulacionAceptada = postulacionRepository
                .findByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())
                .map(p -> p.getEstado() == EstadoPostulacion.ACEPTADO)
                .orElse(false);

        if (!postulacionAceptada) {
            throw new BusinessException("Solo estudiantes aceptados pueden subir entregables");
        }

        var entregable = entregableRepository.save(Entregable.builder()
                .proyecto(proyecto)
                .estudiante(estudiante)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .archivo(request.archivo())
                .build());

        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "Nuevo entregable recibido",
                "El estudiante " + usuario.getNombre() + " subió un entregable para: " + proyecto.getTitulo(),
                TipoNotificacion.PROYECTO,
                "/dashboard/proyectos/" + proyectoId + "/entregables"
        );

        return toResponse(entregable);
    }

    @Transactional(readOnly = true)
    public List<EntregableResponse> listarPorProyecto(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para ver estos entregables", HttpStatus.FORBIDDEN);
        }

        return entregableRepository.findByProyectoIdWithDetails(proyectoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EntregableResponse> misEntregables(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        return entregableRepository.findByEstudianteIdWithDetails(estudiante.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public EntregableResponse revisar(Long proyectoId, Long entregableId,
                                      RevisarEntregableRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para revisar estos entregables", HttpStatus.FORBIDDEN);
        }

        var entregable = entregableRepository.findById(entregableId)
                .orElseThrow(() -> new ResourceNotFoundException("Entregable", entregableId));

        entregable.setEstado(request.estado());
        entregable.setObservaciones(request.observaciones());
        var saved = entregableRepository.save(entregable);

        notificacionService.crearNotificacion(
                entregable.getEstudiante().getUsuario(),
                "Tu entregable fue revisado",
                "Tu entregable \"" + entregable.getTitulo() + "\" fue marcado como " + request.estado().name(),
                TipoNotificacion.PROYECTO,
                "/mis-entregables"
        );

        return toResponse(saved);
    }

    private EntregableResponse toResponse(Entregable e) {
        return new EntregableResponse(
                e.getId(),
                e.getProyecto().getId(),
                e.getProyecto().getTitulo(),
                e.getEstudiante().getId(),
                e.getEstudiante().getUsuario().getNombre(),
                e.getTitulo(),
                e.getDescripcion(),
                e.getArchivo(),
                e.getEstado(),
                e.getObservaciones(),
                e.getFechaEntrega()
        );
    }
}