package com.mypelink.backend.proyectos.application.service;

import com.mypelink.backend.notificaciones.application.service.NotificacionService;
import com.mypelink.backend.shared.domain.enums.TipoNotificacion;
import com.mypelink.backend.proyectos.domain.model.Postulacion;
import com.mypelink.backend.proyectos.domain.model.Proyecto;
import com.mypelink.backend.proyectos.application.dto.*;
import com.mypelink.backend.proyectos.domain.repository.PostulacionRepository;
import com.mypelink.backend.proyectos.domain.repository.ProyectoRepository;
import com.mypelink.backend.shared.domain.enums.EstadoPostulacion;
import com.mypelink.backend.shared.domain.enums.WorkflowEstado;
import com.mypelink.backend.shared.infrastructure.exception.BusinessException;
import com.mypelink.backend.shared.infrastructure.exception.ResourceNotFoundException;
import com.mypelink.backend.usuarios.domain.repository.EstudianteRepository;
import com.mypelink.backend.usuarios.domain.repository.MypeRepository;
import com.mypelink.backend.usuarios.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final PostulacionRepository postulacionRepository;
    private final MypeRepository mypeRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Transactional(readOnly = true)
    public List<ProyectoResponse> listarPorMype(String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));

        return proyectoRepository.findByMypeIdConMype(mype.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProyectoResponse> listarPublicos(Pageable pageable) {
        List<ProyectoResponse> lista = proyectoRepository
                .findPublicosConMype(WorkflowEstado.PENDIENTE)
                .stream()
                .map(this::toResponse)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), lista.size());
        List<ProyectoResponse> page = lista.subList(start, end);

        return new org.springframework.data.domain.PageImpl<>(page, pageable, lista.size());
    }

    public ProyectoResponse obtenerPorId(Long id) {
        return proyectoRepository.findByIdWithMype(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", id));
    }

    @Transactional
    public ProyectoResponse crear(CrearProyectoRequest request, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado para este usuario"));

        var proyecto = Proyecto.builder()
                .mype(mype)
                .titulo(request.titulo())
                .descripcion(request.descripcion())
                .objetivo(request.objetivo())
                .requisitos(request.requisitos())
                .entregablesSugeridos(request.entregablesSugeridos())
                .areaSistemas(request.areaSistemas())
                .cupos(request.cupos() != null ? request.cupos() : 1)
                .fechaInicio(request.fechaInicio())
                .fechaLimite(request.fechaLimite())
                .estado(WorkflowEstado.BORRADOR)
                .build();

        return toResponse(proyectoRepository.save(proyecto));
    }

    @Transactional
    public PostulacionResponse postular(Long proyectoId, PostulacionRequest request, String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (proyecto.getEstado() != WorkflowEstado.PENDIENTE) {
            throw new BusinessException("El proyecto no está disponible para postulaciones");
        }
        if (postulacionRepository.existsByProyectoIdAndEstudianteId(proyectoId, estudiante.getId())) {
            throw new BusinessException("Ya postulaste a este proyecto");
        }
        long proyectosActivos = postulacionRepository.countByEstudianteIdAndEstado(
                estudiante.getId(), EstadoPostulacion.ACEPTADO);
        if (proyectosActivos >= 2) {
            throw new BusinessException("No puedes tener más de 2 proyectos activos simultáneamente");
        }

        var postulacion = postulacionRepository.save(Postulacion.builder()
                .proyecto(proyecto)
                .estudiante(estudiante)
                .mensajePostulacion(request.mensajePostulacion())
                .archivoAdjunto(request.archivoAdjunto())
                .build());
        notificacionService.crearNotificacion(
                proyecto.getMype().getUsuario(),
                "Nueva postulación recibida",
                "El estudiante " + estudiante.getUsuario().getNombre() + " postuló a tu proyecto: " + proyecto.getTitulo(),
                TipoNotificacion.POSTULACION,
                "/dashboard/postulaciones/" + proyecto.getId()
        );

        return toPostulacionResponse(postulacion);
    }

    private ProyectoResponse toResponse(Proyecto p) {
        return new ProyectoResponse(
                p.getId(), p.getTitulo(), p.getDescripcion(), p.getObjetivo(),
                p.getRequisitos(), p.getEntregablesSugeridos(), p.getAreaSistemas(),
                p.getEstado(), p.getCupos(), p.getFechaInicio(), p.getFechaLimite(),
                p.getFechaCreacion(),
                p.getMype() != null ? p.getMype().getNombreComercial() : null,
                p.getMype() != null ? p.getMype().getId() : null
        );
    }

    private PostulacionResponse toPostulacionResponse(Postulacion p) {
        return new PostulacionResponse(
                p.getId(),
                p.getProyecto().getId(),
                p.getProyecto().getTitulo(),
                p.getEstudiante().getId(),
                p.getEstudiante().getUsuario().getNombre(),
                p.getEstado(),
                p.getMensajePostulacion(),
                p.getFechaPostulacion()
        );
    }

    @Transactional
    public ProyectoResponse publicar(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para publicar este proyecto", HttpStatus.FORBIDDEN);
        }
        if (proyecto.getEstado() != WorkflowEstado.BORRADOR) {
            throw new BusinessException("Solo los proyectos en BORRADOR pueden publicarse");
        }

        proyecto.setEstado(WorkflowEstado.PENDIENTE);
        return toResponse(proyectoRepository.save(proyecto));
    }

    public List<PostulacionResponse> listarPostulaciones(Long proyectoId, String emailMype) {
        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para ver estas postulaciones", HttpStatus.FORBIDDEN);
        }

        return postulacionRepository.findByProyectoIdWithDetails(proyectoId)
                .stream()
                .map(this::toPostulacionResponse)
                .toList();
    }

    @Transactional
    public PostulacionResponse cambiarEstadoPostulacion(Long proyectoId, Long postulacionId, CambiarEstadoPostulacionRequest request, String emailMype) {

        var usuario = usuarioRepository.findByEmailWithRole(emailMype)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var mype = mypeRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil MYPE no encontrado"));
        var proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto", proyectoId));

        if (!proyecto.getMype().getId().equals(mype.getId())) {
            throw new BusinessException("No tienes permiso para gestionar estas postulaciones", HttpStatus.FORBIDDEN);
        }

        var postulacion = postulacionRepository.findById(postulacionId)
                .orElseThrow(() -> new ResourceNotFoundException("Postulacion", postulacionId));

        if (request.estado() == EstadoPostulacion.ACEPTADO) {
            long aceptados = postulacionRepository.findByProyectoId(proyectoId)
                    .stream()
                    .filter(p -> p.getEstado() == EstadoPostulacion.ACEPTADO)
                    .count();
            if (aceptados >= proyecto.getCupos()) {
                throw new BusinessException("No hay cupos disponibles en este proyecto");
            }
        }

        postulacion.setEstado(request.estado());
        postulacion.setFechaRespuesta(java.time.LocalDateTime.now());

        var saved = postulacionRepository.save(postulacion);

        String mensajeNotif = request.estado() == EstadoPostulacion.ACEPTADO
                ? "¡Felicitaciones! Tu postulación al proyecto \"" + proyecto.getTitulo() + "\" fue ACEPTADA."
                : "Tu postulación al proyecto \"" + proyecto.getTitulo() + "\" fue rechazada.";

        notificacionService.crearNotificacion(
                postulacion.getEstudiante().getUsuario(),
                "Respuesta a tu postulacion",
                mensajeNotif,
                TipoNotificacion.POSTULACION,
                "/mis-postulaciones"
        );

        return toPostulacionResponse(saved);
    }

    public List<PostulacionResponse> misPostulaciones(String emailEstudiante) {
        var usuario = usuarioRepository.findByEmailWithRole(emailEstudiante)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        var estudiante = estudianteRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new BusinessException("Perfil de estudiante no encontrado"));

        return postulacionRepository.findByEstudianteIdWithDetails(estudiante.getId())
                .stream()
                .map(this::toPostulacionResponse)
                .toList();
    }
}